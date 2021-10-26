/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.fir.diagnostics

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.diagnostics.withSuppressedDiagnostics
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.constructors
import org.jetbrains.kotlin.fir.declarations.delegateFieldsMap
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.classId
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.isSubtypeOf
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.parcelize.OLD_PARCELER_CLASS_ID
import org.jetbrains.kotlin.parcelize.PARCELIZE_CLASS_CLASS_IDS
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

object FirParcelizeClassChecker : FirClassChecker() {
    private val CREATOR_NAME = Name.identifier("CREATOR")
    private val PARCELABLE_CLASS_ID = ClassId(FqName("android.os"), Name.identifier("Parcelable"))

    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        checkParcelableClass(declaration, context, reporter)
        checkParcelerClass(declaration, context, reporter)
    }

    private fun checkParcelableClass(klass: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val symbol = klass.symbol
        if (!symbol.isParcelize(context.session)) return
        val source = klass.source ?: return
        if (klass !is FirRegularClass) {
            reporter.reportOn(source, FirErrorsParcelize.PARCELABLE_SHOULD_BE_CLASS, context)
            return
        }

        val classKind = klass.classKind
        if (classKind == ClassKind.ANNOTATION_CLASS || classKind == ClassKind.INTERFACE && !klass.isSealed) {
            reporter.reportOn(source, FirErrorsParcelize.PARCELABLE_SHOULD_BE_CLASS, context)
            return
        }

        klass.companionObjectSymbol?.let { companionSymbol ->
            if (companionSymbol.classId.shortClassName == CREATOR_NAME) {
                reporter.reportOn(companionSymbol.source, FirErrorsParcelize.CREATOR_DEFINITION_IS_NOT_ALLOWED, context)
            }
        }

        if (classKind == ClassKind.CLASS && klass.isAbstract) {
            reporter.reportOn(source, FirErrorsParcelize.PARCELABLE_SHOULD_BE_INSTANTIABLE, context)
        }

        if (klass.isInner) {
            reporter.reportOn(source, FirErrorsParcelize.PARCELABLE_CANT_BE_INNER_CLASS, context)
        }

        if (klass.isLocal) {
            reporter.reportOn(source, FirErrorsParcelize.PARCELABLE_CANT_BE_LOCAL_CLASS, context)
        }

        val supertypes = lookupSuperTypes(klass, lookupInterfaces = true, deep = true, context.session, substituteTypes = false)
        if (supertypes.none { it.classId == PARCELABLE_CLASS_ID }) {
            reporter.reportOn(source, FirErrorsParcelize.NO_PARCELABLE_SUPERTYPE, context)
        }

        klass.delegateFieldsMap?.forEach { index, _ ->
            val superTypeRef = klass.superTypeRefs[index]
            val superType = superTypeRef.coneType
            val parcelableType = ConeClassLikeTypeImpl(
                ConeClassLikeLookupTagImpl(PARCELABLE_CLASS_ID),
                emptyArray(),
                isNullable = false
            )
            if (superType.isSubtypeOf(parcelableType, context.session)) {
                reporter.reportOn(superTypeRef.source, FirErrorsParcelize.PARCELABLE_DELEGATE_IS_NOT_ALLOWED, context)
            }
        }

        val constructorSymbols = klass.constructors(context.session)
        val primaryConstructorSymbol = constructorSymbols.find { it.isPrimary }
        val secondaryConstructorSymbols = constructorSymbols.filterNot { it.isPrimary }
        if (primaryConstructorSymbol == null && secondaryConstructorSymbols.isNotEmpty()) {
            reporter.reportOn(source, FirErrorsParcelize.PARCELABLE_SHOULD_HAVE_PRIMARY_CONSTRUCTOR, context)
        }
    }

    private fun checkParcelerClass(klass: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (klass !is FirRegularClass || klass.isCompanion) return
        for (superTypeRef in klass.superTypeRefs) {
            withSuppressedDiagnostics(superTypeRef, context) {
                if (superTypeRef.coneType.classId == OLD_PARCELER_CLASS_ID) {
                    val strategy = if (klass.name == SpecialNames.NO_NAME_PROVIDED) {
                        SourceElementPositioningStrategies.OBJECT_KEYWORD
                    } else {
                        SourceElementPositioningStrategies.NAME_IDENTIFIER
                    }
                    reporter.reportOn(klass.source, FirErrorsParcelize.DEPRECATED_PARCELER, context, positioningStrategy = strategy)
                }
            }
        }
    }
}

@OptIn(ExperimentalContracts::class)
fun FirClassSymbol<*>?.isParcelize(session: FirSession): Boolean {
    contract {
        returns(true) implies (this@isParcelize != null)
    }

    if (this == null) return false
    val visited = mutableListOf<FirClassSymbol<*>>()

    fun lookup(symbol: FirClassSymbol<*>): Boolean {
        if (!visited.add(symbol)) return false
        if (symbol.annotations.any { it.classId in PARCELIZE_CLASS_CLASS_IDS }) return true
        return symbol.resolvedSuperTypeRefs.any {
            val superSymbol = it.toRegularClassSymbol(session) ?: return@any false
            lookup(superSymbol)
        }
    }

    return lookup(this)
}
