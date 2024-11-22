package com.stripe.android.financialconnections

import androidx.annotation.RestrictTo

/**
 * Callback that is invoked when a [FinancialConnectionsSheetResult] is available.
 */
fun interface FinancialConnectionsSheetResultCallback {
    fun onFinancialConnectionsSheetResult(financialConnectionsSheetResult: FinancialConnectionsSheetResult)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface FinancialConnectionsSheetInternalResultCallback {
    fun onFinancialConnectionsSheetResult(financialConnectionsSheetResult: FinancialConnectionsSheetInternalResult)
}
