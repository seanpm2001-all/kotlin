/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.light.LightEmptyImplementsList
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.FakeFileForLightClass
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile

abstract class FirLightClassForFacadeBase(
    manager: PsiManager,
    final override val facadeClassFqName: FqName,
    override val files: Collection<KtFile>
) : FirLightClassBase(manager), KtLightClassForFacade {
    private val firstFileInFacade by lazyPub { files.first() }

    override val clsDelegate: PsiClass get() = invalidAccess()

    override fun getScope(): PsiElement = parent

    private val packageFqName: FqName =
        facadeClassFqName.parent()

    private val implementsList: LightEmptyImplementsList =
        LightEmptyImplementsList(manager)

    private val packageClsFile = FakeFileForLightClass(
        firstFileInFacade,
        lightClass = { this },
        stub = { null },
        packageFqName = packageFqName
    )

    override fun getParent(): PsiElement = containingFile

    override val kotlinOrigin: KtClassOrObject? get() = null

    override fun getExtendsList(): PsiReferenceList? = null

    override fun isDeprecated() = false

    override fun isInterface() = false

    override fun isAnnotationType() = false

    override fun isEnum() = false

    override fun getContainingClass(): PsiClass? = null

    override fun getContainingFile() = packageClsFile

    override fun hasTypeParameters() = false

    override fun getTypeParameters(): Array<out PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY

    override fun getTypeParameterList(): PsiTypeParameterList? = null

    override fun getImplementsList() = implementsList

    override fun getInterfaces(): Array<out PsiClass> = PsiClass.EMPTY_ARRAY

    override fun getInnerClasses(): Array<out PsiClass> = PsiClass.EMPTY_ARRAY

    override fun getOwnInnerClasses(): List<PsiClass> = listOf()

    override fun getAllInnerClasses(): Array<PsiClass> = PsiClass.EMPTY_ARRAY

    override fun findInnerClassByName(@NonNls name: String, checkBases: Boolean): PsiClass? = null

    override fun isInheritorDeep(baseClass: PsiClass?, classToByPass: PsiClass?): Boolean = false

    override fun getName() = super<KtLightClassForFacade>.getName()

    override fun getQualifiedName() = facadeClassFqName.asString()

    override fun getNameIdentifier(): PsiIdentifier? = null

    override fun isValid() = files.all { it.isValid && it.hasTopLevelCallables() && facadeClassFqName == it.javaFileFacadeFqName }

    override fun getNavigationElement() = firstFileInFacade

    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean {
        return baseClass.qualifiedName == CommonClassNames.JAVA_LANG_OBJECT
    }

    override fun getSuperClass(): PsiClass? {
        return JavaPsiFacade.getInstance(project).findClass(CommonClassNames.JAVA_LANG_OBJECT, resolveScope)
    }

    override fun getSupers(): Array<PsiClass> =
        superClass?.let { arrayOf(it) } ?: arrayOf()

    override fun getSuperTypes(): Array<PsiClassType> =
        arrayOf(PsiType.getJavaLangObject(manager, resolveScope))

    override fun hashCode() = facadeClassFqName.hashCode()

    abstract override fun equals(other: Any?): Boolean

    override fun getText() = firstFileInFacade.text ?: ""

    override fun getTextRange(): TextRange = firstFileInFacade.textRange ?: TextRange.EMPTY_RANGE

    override fun getTextOffset() = firstFileInFacade.textOffset

    override fun getStartOffsetInParent() = firstFileInFacade.startOffsetInParent

    override fun isWritable() = files.all { it.isWritable }
}
