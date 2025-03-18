package com.stripe.android.financialconnections

/**
 * Callback that is invoked when a [FinancialConnectionsSheetResult] is available.
 */
fun interface FinancialConnectionsSheetResultCallback {
    fun onFinancialConnectionsSheetResult(financialConnectionsSheetResult: FinancialConnectionsSheetResult)
}
