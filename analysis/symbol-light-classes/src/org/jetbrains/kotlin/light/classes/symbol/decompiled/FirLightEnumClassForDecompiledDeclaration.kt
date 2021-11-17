/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.psi.KtClassOrObject

internal class FirLightEnumClassForDecompiledDeclaration(
    private val psiConstantInitializer: PsiEnumConstantInitializer,
    private val enumConstant: FirLightEnumEntryForDecompiledDeclaration,
    containingClass: KtLightClass,
    file: PsiFile,
    kotlinOrigin: KtClassOrObject?
) : FirLightClassForDecompiledDeclaration(
    psiConstantInitializer,
    containingClass,
    file,
    kotlinOrigin
), PsiEnumConstantInitializer {
    override fun getBaseClassType(): PsiClassType = psiConstantInitializer.baseClassType

    override fun getArgumentList(): PsiExpressionList? = psiConstantInitializer.argumentList

    override fun getEnumConstant(): PsiEnumConstant = enumConstant

    override fun getBaseClassReference(): PsiJavaCodeReferenceElement = psiConstantInitializer.baseClassReference

    override fun isInQualifiedNew(): Boolean = psiConstantInitializer.isInQualifiedNew
}
