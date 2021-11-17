/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.lazyPub

internal class FirLightEnumEntryForDecompiledDeclaration(
    override val clsDelegate: PsiEnumConstant,
    containingClass: KtLightClass,
    override val lightMemberOrigin: LightMemberOrigin?,
    file: PsiFile
) : FirLightFieldForDecompiledDeclaration(clsDelegate, containingClass, lightMemberOrigin), PsiEnumConstant {
    private val _initializingClass: PsiEnumConstantInitializer? by lazyPub {
        clsDelegate.initializingClass?.let {
            FirLightEnumClassForDecompiledDeclaration(
                psiConstantInitializer = it,
                enumConstant = this,
                containingClass = containingClass,
                file = file,
                kotlinOrigin = null
            )
        }
    }

    override fun getArgumentList(): PsiExpressionList? = clsDelegate.argumentList

    override fun resolveConstructor(): PsiMethod? = clsDelegate.resolveConstructor()

    override fun resolveMethod(): PsiMethod? = clsDelegate.resolveMethod()

    override fun resolveMethodGenerics(): JavaResolveResult = clsDelegate.resolveMethodGenerics()

    override fun getInitializingClass(): PsiEnumConstantInitializer? = _initializingClass

    override fun getOrCreateInitializingClass(): PsiEnumConstantInitializer =
        _initializingClass ?: error("cannot create initializing class in light enum constant")
}
