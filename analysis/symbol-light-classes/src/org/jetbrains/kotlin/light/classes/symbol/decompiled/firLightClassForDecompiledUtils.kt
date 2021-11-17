/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.decompiled

import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.providers.createProjectWideOutOfBlockModificationTracker
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.shouldNotBeVisibleAsLightClass
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.light.classes.symbol.FirLightClassForFacadeBase
import org.jetbrains.kotlin.light.classes.symbol.caches.SymbolLightClassFacadeCache
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.checkWithAttachment

internal fun getOrCreateFirLightClassForDeserialized(decompiledClassOrObject: KtClassOrObject): KtLightClass? =
    CachedValuesManager.getCachedValue(decompiledClassOrObject) {
        CachedValueProvider.Result
            .create(
                createFirLightClassForDeserializedNoCache(decompiledClassOrObject),
                decompiledClassOrObject.project.createProjectWideOutOfBlockModificationTracker()
            )
    }

private fun createFirLightClassForDeserializedNoCache(decompiledClassOrObject: KtClassOrObject): KtLightClass? {
    val containingFile = decompiledClassOrObject.containingFile

    if (containingFile !is KtFile || !containingFile.isCompiled) return null

    if (decompiledClassOrObject.shouldNotBeVisibleAsLightClass()) {
        return null
    }

    if (decompiledClassOrObject is KtEnumEntry) {
        return null
    }

    val rootLightClassForDecompiledFile = getOrCreateLightClassForDecompiledKotlinFile(containingFile) ?: return null
    return findCorrespondingLightClass(decompiledClassOrObject, rootLightClassForDecompiledFile)
}

private fun getOrCreateLightClassForDecompiledKotlinFile(file: KtFile): FirLightClassForFacadeBase? {
    // `ClsFileImpl.buildFileStub` expects to load class name from the given .class file.
    // Technically, we need to check specific header via `ClassReader`, but perhaps too much here.
    // Instead, checking file path name seems to work.
    return if (!file.name.endsWith(".class")) null else
        file.project.getService(SymbolLightClassFacadeCache::class.java)
            .getOrCreateSymbolLightFacade(listOf(file), file.javaFileFacadeFqName)
}

private fun findCorrespondingLightClass(
    decompiledClassOrObject: KtClassOrObject,
    rootLightClassForDecompiledFile: FirLightClassForFacadeBase,
): KtLightClass? {
    val relativeFqName = getClassRelativeName(decompiledClassOrObject) ?: return null
    val iterator = relativeFqName.pathSegments().iterator()
    val base = iterator.next()

    // In case class files have been obfuscated (i.e., SomeClass belongs to a.class file), just ignore them
    if (rootLightClassForDecompiledFile.name != base.asString()) return null

    var current: KtLightClass = rootLightClassForDecompiledFile
    while (iterator.hasNext()) {
        val name = iterator.next()
        val innerClass = current.findInnerClassByName(name.asString(), false)
        checkWithAttachment(
            innerClass != null,
            { "Could not find corresponding inner/nested class " + relativeFqName + " in class " + decompiledClassOrObject.fqName + "\nFile: " + decompiledClassOrObject.containingKtFile.virtualFile.name },
            {
                it.withAttachment("decompiledClassOrObject", decompiledClassOrObject.text)
                it.withAttachment("fileClass", decompiledClassOrObject.containingFile::class)
                it.withAttachment("file", decompiledClassOrObject.containingFile.text)
                it.withAttachment("root", rootLightClassForDecompiledFile.text)
            },
        )

        current = innerClass as KtLightClass
    }
    return current
}

private fun getClassRelativeName(decompiledClassOrObject: KtClassOrObject): FqName? {
    val name = decompiledClassOrObject.nameAsName ?: return null
    val parent = PsiTreeUtil.getParentOfType(
        decompiledClassOrObject,
        KtClassOrObject::class.java,
        true
    )
    if (parent == null) {
        assert(decompiledClassOrObject.isTopLevel())
        return FqName.topLevel(name)
    }
    return getClassRelativeName(parent)?.child(name)
}
