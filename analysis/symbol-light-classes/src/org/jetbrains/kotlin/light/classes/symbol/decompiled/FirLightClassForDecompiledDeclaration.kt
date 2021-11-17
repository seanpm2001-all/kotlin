/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.openapi.util.Pair
import com.intellij.psi.*
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.impl.PsiImplUtil
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.util.PsiUtil
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.asJava.classes.KotlinClassInnerStuffCache
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.psi.KtClassOrObject

internal open class FirLightClassForDecompiledDeclaration(
    override val clsDelegate: PsiClass,
    private val clsParent: PsiElement,
    private val file: PsiFile,
    override val kotlinOrigin: KtClassOrObject?
) : FirLightClassBase(clsParent.manager) {
    override fun getOwnMethods(): MutableList<PsiMethod> = _methods

    override fun getOwnFields(): MutableList<PsiField> = _fields

    override fun getOwnInnerClasses(): MutableList<PsiClass> = _innerClasses

    override fun hasModifierProperty(name: String): Boolean =
        clsDelegate.hasModifierProperty(name)

    override fun findMethodBySignature(patternMethod: PsiMethod?, checkBases: Boolean): PsiMethod? =
        patternMethod?.let { PsiClassImplUtil.findMethodBySignature(this, it, checkBases) }

    override fun findMethodsBySignature(patternMethod: PsiMethod?, checkBases: Boolean): Array<PsiMethod?> =
        patternMethod?.let { PsiClassImplUtil.findMethodsBySignature(this, it, checkBases) } ?: emptyArray()

    override fun findMethodsAndTheirSubstitutorsByName(
        @NonNls name: String?,
        checkBases: Boolean
    ): List<Pair<PsiMethod?, PsiSubstitutor?>?> =
        PsiClassImplUtil.findMethodsAndTheirSubstitutorsByName(this, name, checkBases)

    override fun getImplementsList(): PsiReferenceList? = clsDelegate.implementsList

    override fun getRBrace(): PsiElement? = null

    override fun getLBrace(): PsiElement? = null

    override fun getInitializers(): Array<PsiClassInitializer> = clsDelegate.initializers

    override fun getContainingClass(): PsiClass? = parent as? PsiClass

    override fun isInheritorDeep(baseClass: PsiClass?, classToByPass: PsiClass?): Boolean =
        clsDelegate.isInheritorDeep(baseClass, classToByPass)

    override fun getAllMethodsAndTheirSubstitutors(): List<Pair<PsiMethod?, PsiSubstitutor?>?> =
        PsiClassImplUtil.getAllWithSubstitutorsByMap<PsiMethod>(this, PsiClassImplUtil.MemberType.METHOD)

    override fun isInterface(): Boolean = clsDelegate.isInterface

    override fun getTypeParameters(): Array<PsiTypeParameter> =
        clsDelegate.typeParameters

    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean =
        clsDelegate.isInheritor(baseClass, checkDeep)

    override fun processDeclarations(
        processor: PsiScopeProcessor,
        state: ResolveState,
        lastParent: PsiElement?,
        place: PsiElement
    ): Boolean {
        if (isEnum) {
            if (!KotlinClassInnerStuffCache.processDeclarationsInEnum(processor, state, myInnersCache)) return false
        }
        return PsiClassImplUtil.processDeclarationsInClass(
            this, processor, state, null,
            lastParent, place, PsiUtil.getLanguageLevel(place), false
        )
    }

    override fun isEnum(): Boolean = clsDelegate.isEnum

    override fun getExtendsListTypes(): Array<PsiClassType?> =
        PsiClassImplUtil.getExtendsListTypes(this)

    override fun getTypeParameterList(): PsiTypeParameterList? = clsDelegate.typeParameterList

    override fun isAnnotationType(): Boolean = clsDelegate.isAnnotationType

    override fun getNameIdentifier(): PsiIdentifier? = clsDelegate.nameIdentifier

    override fun getInterfaces(): Array<PsiClass> =
        PsiClassImplUtil.getInterfaces(this)

    override fun getSuperClass(): PsiClass? =
        PsiClassImplUtil.getSuperClass(this)

    override fun getSupers(): Array<PsiClass> =
        PsiClassImplUtil.getSupers(this)

    override fun getSuperTypes(): Array<PsiClassType> =
        PsiClassImplUtil.getSuperTypes(this)

    override fun getVisibleSignatures(): MutableCollection<HierarchicalMethodSignature> =
        PsiSuperMethodImplUtil.getVisibleSignatures(this)

    override fun getQualifiedName(): String? = clsDelegate.qualifiedName

    override fun getImplementsListTypes(): Array<PsiClassType?> =
        PsiClassImplUtil.getImplementsListTypes(this)

    override fun isDeprecated(): Boolean = clsDelegate.isDeprecated

    override fun setName(name: String): PsiElement = clsDelegate.setName(name)

    override fun hasTypeParameters(): Boolean =
        PsiImplUtil.hasTypeParameters(this)

    override fun getExtendsList(): PsiReferenceList? = clsDelegate.extendsList

    override fun getDocComment(): PsiDocComment? = clsDelegate.docComment

    override fun getModifierList(): PsiModifierList? = clsDelegate.modifierList

    override fun getScope(): PsiElement = clsDelegate.scope

    override fun getAllInnerClasses(): Array<PsiClass> = PsiClassImplUtil.getAllInnerClasses(this)

    override fun getAllMethods(): Array<PsiMethod> = PsiClassImplUtil.getAllMethods(this)

    override fun getAllFields(): Array<PsiField> = PsiClassImplUtil.getAllFields(this)

    private val _methods: MutableList<PsiMethod> by lazyPub {
        mutableListOf<PsiMethod>().also {
            clsDelegate.methods.mapTo(it) { psiMethod ->
                FirLightMethodForDecompiledDeclaration(
                    clsDelegate = psiMethod,
                    containingClass = this,
                    lightMemberOrigin = null, // TODO: LightMemberOriginForCompiledMethod(psiMethod, file),
                )
            }
        }
    }

    private val _fields: MutableList<PsiField> by lazyPub {
        mutableListOf<PsiField>().also {
            clsDelegate.fields.mapTo(it) { psiField ->
                if (psiField !is PsiEnumConstant) {
                    FirLightFieldForDecompiledDeclaration(
                        clsDelegate = psiField,
                        containingClass = this,
                        lightMemberOrigin = null, // TODO: LightMemberOriginForCompiledField(psiField, file),
                    )
                } else {
                    FirLightEnumEntryForDecompiledDeclaration(
                        clsDelegate = psiField,
                        containingClass = this,
                        lightMemberOrigin = null, // TODO: LightMemberOriginForCompiledField(psiField, file),
                        file = psiField.containingFile
                    )
                }
            }
        }
    }

    private val _innerClasses: MutableList<PsiClass> by lazyPub {
        mutableListOf<PsiClass>().also {
            clsDelegate.innerClasses.mapTo(it) { psiClass ->
                val innerDeclaration = kotlinOrigin
                    ?.declarations
                    ?.filterIsInstance<KtClassOrObject>()
                    ?.firstOrNull { cls -> cls.name == clsDelegate.name }

                FirLightClassForDecompiledDeclaration(
                    clsDelegate = psiClass,
                    clsParent = this,
                    file = psiClass.containingFile,
                    kotlinOrigin = innerDeclaration,
                )
            }
        }
    }

    override val originKind: LightClassOriginKind = LightClassOriginKind.BINARY

    override fun getNavigationElement() = kotlinOrigin?.navigationElement ?: file

    override fun equals(other: Any?): Boolean {
        return this === other || other is FirLightClassForDecompiledDeclaration &&
                qualifiedName == other.qualifiedName &&
                kotlinOrigin?.fqName == other.kotlinOrigin?.fqName
    }

    override fun hashCode(): Int = qualifiedName?.hashCode() ?: kotlinOrigin?.fqName?.hashCode() ?: 0

    override fun copy(): FirLightClassForDecompiledDeclaration =
        FirLightClassForDecompiledDeclaration(clsDelegate, clsParent, file, kotlinOrigin)

    override fun toString(): String = "${this.javaClass.simpleName} of $parent"

    override fun getName(): String? = clsDelegate.name

    override fun isValid(): Boolean = file.isValid && clsDelegate.isValid && (kotlinOrigin?.isValid != false)
}
