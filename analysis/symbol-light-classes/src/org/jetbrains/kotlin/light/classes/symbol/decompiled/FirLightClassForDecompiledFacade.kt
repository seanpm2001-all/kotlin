/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.*
import com.intellij.psi.impl.light.LightModifierList
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile

internal class FirLightClassForDecompiledFacade(
    override val clsDelegate: PsiClass,
    manager: PsiManager,
    facadeClassFqName: FqName,
    files: Collection<KtFile>
) : FirLightClassForFacadeBase(manager, facadeClassFqName, files) {
    init {
        require(files.all { it.isCompiled })
    }

    private val multiFileClass: Boolean by lazyPub {
        files.size > 1 // TODO: files.any { it.annotations.any { it.hasJvmMultifileClassAnnotation() } }
    }

    private val _modifierList: PsiModifierList by lazyPub {
        if (multiFileClass)
            return@lazyPub LightModifierList(manager, KotlinLanguage.INSTANCE, PsiModifier.PUBLIC, PsiModifier.FINAL)

        val modifiers = setOf(PsiModifier.PUBLIC, PsiModifier.FINAL)
        val annotations = emptyList<PsiAnnotation>() // TODO
        FirLightClassModifierList(this@FirLightClassForDecompiledFacade, modifiers, annotations)
    }

    override fun getModifierList(): PsiModifierList = _modifierList

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

    override fun getOwnFields() = _fields

    override fun getOwnMethods() = _methods

    override fun getOwnInnerClasses() = _innerClasses

    override fun copy(): FirLightClassForDecompiledFacade =
        FirLightClassForDecompiledFacade(clsDelegate, manager, facadeClassFqName, files)

    override fun hasModifierProperty(@NonNls name: String) = _modifierList.hasModifierProperty(name)

    override fun isEquivalentTo(another: PsiElement?): Boolean =
        equals(another) || another is FirLightClassForDecompiledFacade && another.qualifiedName == qualifiedName

    override fun equals(other: Any?): Boolean {
        if (other !is FirLightClassForDecompiledFacade) return false
        if (this === other) return true

        if (this.hashCode() != other.hashCode()) return false
        if (manager != other.manager) return false
        if (facadeClassFqName != other.facadeClassFqName) return false
        return true
    }

    override fun toString() = "${FirLightClassForDecompiledFacade::class.java.simpleName}:$facadeClassFqName"

    override val originKind: LightClassOriginKind
        get() = LightClassOriginKind.BINARY

    override fun isValid(): Boolean = clsDelegate.isValid && (kotlinOrigin?.isValid != false)
}
