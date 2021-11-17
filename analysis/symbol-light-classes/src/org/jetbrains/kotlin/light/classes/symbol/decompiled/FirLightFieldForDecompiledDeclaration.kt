/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.*
import com.intellij.psi.impl.PsiVariableEx
import com.intellij.psi.javadoc.PsiDocComment
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.psi.KtDeclaration

internal open class FirLightFieldForDecompiledDeclaration(
    override val clsDelegate: PsiField,
    private val containingClass: KtLightClass,
    override val lightMemberOrigin: LightMemberOrigin?,
) : FirLightField(containingClass, lightMemberOrigin) {
    override val kotlinOrigin: KtDeclaration? get() = lightMemberOrigin?.originalElement

    override fun hasModifierProperty(name: String): Boolean = clsDelegate.hasModifierProperty(name)

    override fun getContainingClass(): KtLightClass = containingClass

    override fun normalizeDeclaration() = clsDelegate.normalizeDeclaration()

    override fun getNameIdentifier(): PsiIdentifier = clsDelegate.nameIdentifier

    override fun getName(): String = clsDelegate.name

    override fun getInitializer(): PsiExpression? = clsDelegate.initializer

    override fun getDocComment(): PsiDocComment? = clsDelegate.docComment

    override fun getTypeElement(): PsiTypeElement? = clsDelegate.typeElement

    override fun getModifierList(): PsiModifierList? = clsDelegate.modifierList

    override fun hasInitializer(): Boolean = clsDelegate.hasInitializer()

    override fun getType(): PsiType = clsDelegate.type

    override fun isDeprecated(): Boolean = clsDelegate.isDeprecated

    override fun setName(name: String): PsiElement = clsDelegate.setName(name)

    override fun computeConstantValue(): Any? = clsDelegate.computeConstantValue()

    override fun computeConstantValue(visitedVars: MutableSet<PsiVariable>?): Any? =
        (clsDelegate as? PsiVariableEx)?.computeConstantValue(visitedVars)

    override fun equals(other: Any?): Boolean =
        other is FirLightFieldForDecompiledDeclaration &&
            name == other.name &&
            containingClass == other.containingClass &&
            clsDelegate == other.clsDelegate

    override fun hashCode(): Int = name.hashCode()

    override fun copy(): FirLightFieldForDecompiledDeclaration =
        FirLightFieldForDecompiledDeclaration(clsDelegate, containingClass, lightMemberOrigin)

    override fun toString(): String = "${this.javaClass.simpleName} of $containingClass"

    override fun isValid(): Boolean = parent.isValid
}
