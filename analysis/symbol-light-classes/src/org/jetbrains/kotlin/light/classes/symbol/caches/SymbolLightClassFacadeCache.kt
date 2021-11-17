/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.caches

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.ClassFileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.compiled.ClsClassImpl
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.impl.java.stubs.impl.PsiJavaFileStubImpl
import org.jetbrains.kotlin.analysis.providers.createProjectWideOutOfBlockModificationTracker
import org.jetbrains.kotlin.light.classes.symbol.FirLightClassForDecompiledFacade
import org.jetbrains.kotlin.light.classes.symbol.FirLightClassForFacadeBase
import org.jetbrains.kotlin.light.classes.symbol.FirLightClassForFacade
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.analysis.utils.caches.*
import org.jetbrains.kotlin.asJava.builder.ClsWrapperStubPsiFactory
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import java.util.concurrent.ConcurrentHashMap

class SymbolLightClassFacadeCache(project: Project) {
    private val cache by softCachedValue(
        project,
        project.createProjectWideOutOfBlockModificationTracker()
    ) {
        ConcurrentHashMap<FacadeKey, FirLightClassForFacadeBase>()
    }

    // TODO: need to use modification timestamp as part of key?
    private val stubCache by softCachedValue(
        project,
        project.createProjectWideOutOfBlockModificationTracker()
    ) {
        ConcurrentHashMap<VirtualFile, PsiJavaFileStubImpl>()
    }

    fun getOrCreateSymbolLightFacade(
        ktFiles: List<KtFile>,
        facadeClassFqName: FqName,
    ): FirLightClassForFacadeBase? {
        if (ktFiles.isEmpty()) return null
        val key = FacadeKey(facadeClassFqName, ktFiles.toSet())
        return cache.computeIfAbsent(key) {
            getOrCreateFirLightFacadeNoCache(ktFiles, facadeClassFqName)
        }
    }

    private fun getOrCreateFirLightFacadeNoCache(
        ktFiles: List<KtFile>,
        facadeClassFqName: FqName,
    ): FirLightClassForFacadeBase {
        val firstFile = ktFiles.first()
        return when {
            ktFiles.none { it.isCompiled } ->
                FirLightClassForFacade(firstFile.manager, facadeClassFqName, ktFiles)
            ktFiles.all { it.isCompiled } -> {
                val file = ktFiles.firstOrNull { it.javaFileFacadeFqName == facadeClassFqName }
                    ?: error("Can't find the representative decompiled file for $facadeClassFqName")
                val classOrObject = file.declarations.filterIsInstance<KtClassOrObject>().singleOrNull()
                val clsDelegate = createClsJavaClassFromVirtualFile(
                    mirrorFile = file,
                    classFile = file.virtualFile,
                    correspondingClassOrObject = classOrObject
                )
                FirLightClassForDecompiledFacade(clsDelegate, firstFile.manager, facadeClassFqName, ktFiles)
            }
            else ->
                error("Source and compiled files are mixed: $ktFiles}")
        }
    }

    private fun createClsJavaClassFromVirtualFile(
        mirrorFile: KtFile,
        classFile: VirtualFile,
        correspondingClassOrObject: KtClassOrObject?
    ): ClsClassImpl {
        val javaFileStub = stubCache.computeIfAbsent(classFile) {
            ClsFileImpl.buildFileStub(classFile, classFile.contentsToByteArray(false)) as PsiJavaFileStubImpl
        }
        javaFileStub.psiFactory = ClsWrapperStubPsiFactory.INSTANCE
        val manager = PsiManager.getInstance(mirrorFile.project)
        val fakeFile = object : ClsFileImpl(ClassFileViewProvider(manager, classFile)) {
            override fun getNavigationElement(): PsiElement {
                if (correspondingClassOrObject != null) {
                    return correspondingClassOrObject.navigationElement.containingFile
                }
                return super.getNavigationElement()
            }

            override fun getStub() = javaFileStub

            override fun getMirror() = mirrorFile

            override fun isPhysical() = false
        }
        javaFileStub.psi = fakeFile
        return fakeFile.classes.single() as ClsClassImpl
    }

    private data class FacadeKey(val fqName: FqName, val files: Set<KtFile>)
}
