/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.SourceNavigator
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.toClassLikeSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.REDUNDANT_NULLABLE
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*

object FirRedundantNullableChecker : FirTypeRefChecker() {
    override fun check(typeRef: FirTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        if (typeRef.isMarkedNullable != true) return

        if (typeRef is FirResolvedTypeRef) {
            var symbol = typeRef.toClassLikeSymbol(context.session)
            while (symbol is FirTypeAliasSymbol) {
                val resolvedExpandedTypeRef = symbol.resolvedExpandedTypeRef
                if (resolvedExpandedTypeRef.type.isMarkedNullable) {
                    reporter.reportOn(typeRef.source, REDUNDANT_NULLABLE, context)
                    break
                } else {
                    symbol = resolvedExpandedTypeRef.toClassLikeSymbol(context.session)
                }
            }
        } else if (typeRef is FirUserTypeRef) {
            with(SourceNavigator.forElement(typeRef)) {
                if (typeRef.isRedundantNullable()) {
                    reporter.reportOn(typeRef.source, REDUNDANT_NULLABLE, context)
                }
            }
        }
    }
}