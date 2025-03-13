package com.stripe.android.financialconnections

/**
 * Callback that is invoked when a [FinancialConnectionsSheetForTokenResult] is available.
 */
fun interface FinancialConnectionsSheetResultForTokenCallback {
    fun onFinancialConnectionsSheetResult(
        financialConnectionsSheetForTokenResult: FinancialConnectionsSheetForTokenResult
    )
}
