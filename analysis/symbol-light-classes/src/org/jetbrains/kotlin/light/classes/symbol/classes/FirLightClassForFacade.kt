/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.*
import com.intellij.psi.impl.light.LightModifierList
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.light.classes.symbol.classes.analyseForLightClasses
import org.jetbrains.kotlin.light.classes.symbol.classes.createField
import org.jetbrains.kotlin.light.classes.symbol.classes.createMethods
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile

class FirLightClassForFacade(
    manager: PsiManager,
    facadeClassFqName: FqName,
    files: Collection<KtFile>
) : FirLightClassForFacadeBase(manager, facadeClassFqName, files) {

    init {
        require(files.isNotEmpty())
        /*
        Actually, here should be the following check
        require(files.all { it.getKtModule() is KtSourceModule })
        but it is quite expensive
         */
        require(files.none { it.isCompiled })
    }

    private val fileSymbols by lazyPub {
        files.map { ktFile ->
            analyseForLightClasses(ktFile) {
                ktFile.getFileSymbol()
            }
        }
    }

    private val _modifierList: PsiModifierList by lazyPub {
        if (multiFileClass)
            return@lazyPub LightModifierList(manager, KotlinLanguage.INSTANCE, PsiModifier.PUBLIC, PsiModifier.FINAL)

        val modifiers = setOf(PsiModifier.PUBLIC, PsiModifier.FINAL)

        val annotations = fileSymbols.flatMap {
            it.computeAnnotations(
                this@FirLightClassForFacade,
                NullabilityType.Unknown,
                AnnotationUseSiteTarget.FILE,
                includeAnnotationsWithoutSite = false
            )
        }

        FirLightClassModifierList(this@FirLightClassForFacade, modifiers, annotations)
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    private val _ownMethods: List<KtLightMethod> by lazyPub {
        val result = mutableListOf<KtLightMethod>()

        val methodsAndProperties = sequence<KtCallableSymbol> {
            for (fileSymbol in fileSymbols) {
                analyzeWithSymbolAsContext(fileSymbol) {
                    for (callableSymbol in fileSymbol.getFileScope().getCallableSymbols()) {
                        if (callableSymbol !is KtFunctionSymbol && callableSymbol !is KtKotlinPropertySymbol) continue
                        if (callableSymbol !is KtSymbolWithVisibility) continue
                        val isPrivate = callableSymbol.toPsiVisibilityForMember(isTopLevel = true) == PsiModifier.PRIVATE
                        if (isPrivate && multiFileClass) continue
                        yield(callableSymbol)
                    }
                }
            }
        }
        createMethods(methodsAndProperties, result, isTopLevel = true)

        result
    }

    private val multiFileClass: Boolean by lazyPub {
        files.size > 1 || fileSymbols.any { it.hasJvmMultifileClassAnnotation() }
    }

    private fun loadFieldsFromFile(
        fileScope: KtScope,
        nameGenerator: FirLightField.FieldNameGenerator,
        result: MutableList<KtLightField>
    ) {
        for (propertySymbol in fileScope.getCallableSymbols()) {

            if (propertySymbol !is KtKotlinPropertySymbol) continue

            if (propertySymbol.isConst && multiFileClass) continue

            val isLateInitWithPublicAccessors = if (propertySymbol.isLateInit) {
                val getterIsPublic = propertySymbol.getter?.toPsiVisibilityForMember(isTopLevel = true)
                    ?.let { it == PsiModifier.PUBLIC } ?: true
                val setterIsPublic = propertySymbol.setter?.toPsiVisibilityForMember(isTopLevel = true)
                    ?.let { it == PsiModifier.PUBLIC } ?: true
                getterIsPublic && setterIsPublic
            } else false

            val forceStaticAndPropertyVisibility = isLateInitWithPublicAccessors ||
                    (propertySymbol.isConst) ||
                    propertySymbol.hasJvmFieldAnnotation()

            createField(
                propertySymbol,
                nameGenerator,
                isTopLevel = true,
                forceStatic = forceStaticAndPropertyVisibility,
                takePropertyVisibility = forceStaticAndPropertyVisibility,
                result
            )
        }
    }

    private val _ownFields: List<KtLightField> by lazyPub {
        val result = mutableListOf<KtLightField>()
        val nameGenerator = FirLightField.FieldNameGenerator()
        for (fileSymbol in fileSymbols) {
            analyzeWithSymbolAsContext(fileSymbol) {
                loadFieldsFromFile(fileSymbol.getFileScope(), nameGenerator, result)
            }
        }
        result
    }

    override fun getOwnFields() = _ownFields

    override fun getOwnMethods() = _ownMethods

    override fun copy(): FirLightClassForFacade =
        FirLightClassForFacade(manager, facadeClassFqName, files)

    override fun hasModifierProperty(@NonNls name: String) = _modifierList.hasModifierProperty(name)

    override fun isEquivalentTo(another: PsiElement?): Boolean =
        equals(another) || another is FirLightClassForFacade && another.qualifiedName == qualifiedName

    override fun equals(other: Any?): Boolean {
        if (other !is FirLightClassForFacade) return false
        if (this === other) return true

        if (this.hashCode() != other.hashCode()) return false
        if (manager != other.manager) return false
        if (facadeClassFqName != other.facadeClassFqName) return false
        if (!fileSymbols.containsAll(other.fileSymbols)) return false
        if (!other.fileSymbols.containsAll(fileSymbols)) return false
        return true
    }

    override fun toString() = "${FirLightClassForFacade::class.java.simpleName}:$facadeClassFqName"

    override val originKind: LightClassOriginKind
        get() = LightClassOriginKind.SOURCE
}
