/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.*
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.util.MethodSignature
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_BASE
import org.jetbrains.kotlin.psi.KtDeclaration

internal class FirLightMethodForDecompiledDeclaration(
    override val clsDelegate: PsiMethod,
    private val containingClass: KtLightClass,
    override val lightMemberOrigin: LightMemberOrigin?,
) : FirLightMethod(lightMemberOrigin, containingClass, METHOD_INDEX_BASE) {

    override val kotlinOrigin: KtDeclaration? get() = lightMemberOrigin?.originalElement

    override fun getContainingClass(): KtLightClass = containingClass

    override fun hasModifierProperty(name: String): Boolean = clsDelegate.hasModifierProperty(name)

    override fun getReturnTypeElement(): PsiTypeElement? = clsDelegate.returnTypeElement

    override fun getTypeParameters(): Array<PsiTypeParameter> = clsDelegate.typeParameters

    override fun getThrowsList(): PsiReferenceList = clsDelegate.throwsList

    override fun getReturnType(): PsiType? = clsDelegate.returnType

    override fun hasTypeParameters(): Boolean = clsDelegate.hasTypeParameters()

    override fun getTypeParameterList(): PsiTypeParameterList? = clsDelegate.typeParameterList

    override fun isVarArgs(): Boolean = clsDelegate.isVarArgs

    override fun isConstructor(): Boolean = clsDelegate.isConstructor

    override fun getNameIdentifier(): PsiIdentifier? = clsDelegate.nameIdentifier

    override fun getName(): String = clsDelegate.name

    override fun getDocComment(): PsiDocComment? = clsDelegate.docComment

    override fun getModifierList(): PsiModifierList = clsDelegate.modifierList

    override fun getBody(): PsiCodeBlock? = null

    override fun getDefaultValue(): PsiAnnotationMemberValue? = (clsDelegate as? PsiAnnotationMethod)?.defaultValue

    override fun isDeprecated(): Boolean = clsDelegate.isDeprecated

    override fun getParameterList(): PsiParameterList = clsDelegate.parameterList

    override fun getHierarchicalMethodSignature() = PsiSuperMethodImplUtil.getHierarchicalMethodSignature(this)

    override fun findSuperMethodSignaturesIncludingStatic(checkAccess: Boolean): List<MethodSignatureBackedByPsiMethod> =
        PsiSuperMethodImplUtil.findSuperMethodSignaturesIncludingStatic(this, checkAccess)

    override fun findDeepestSuperMethod() = PsiSuperMethodImplUtil.findDeepestSuperMethod(this)

    override fun findDeepestSuperMethods(): Array<out PsiMethod> = PsiSuperMethodImplUtil.findDeepestSuperMethods(this)

    override fun findSuperMethods(): Array<out PsiMethod> = PsiSuperMethodImplUtil.findSuperMethods(this)

    override fun findSuperMethods(checkAccess: Boolean): Array<out PsiMethod> =
        PsiSuperMethodImplUtil.findSuperMethods(this, checkAccess)

    override fun findSuperMethods(parentClass: PsiClass?): Array<out PsiMethod> =
        PsiSuperMethodImplUtil.findSuperMethods(this, parentClass)

    override fun getSignature(substitutor: PsiSubstitutor): MethodSignature =
        MethodSignatureBackedByPsiMethod.create(this, substitutor)

    override fun equals(other: Any?): Boolean =
        other is FirLightMethodForDecompiledDeclaration &&
            name == other.name &&
            containingClass == other.containingClass &&
            clsDelegate == other.clsDelegate

    override fun hashCode(): Int = name.hashCode()

    override fun copy(): FirLightMethodForDecompiledDeclaration =
        FirLightMethodForDecompiledDeclaration(clsDelegate, containingClass, lightMemberOrigin)

    override fun toString(): String = "${this.javaClass.simpleName} of $containingClass"

    override fun isValid(): Boolean = parent.isValid
}
