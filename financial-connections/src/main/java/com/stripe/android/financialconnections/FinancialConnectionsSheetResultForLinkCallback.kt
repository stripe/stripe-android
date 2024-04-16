package com.stripe.android.financialconnections

import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetLinkResult

/**
 * Callback that is invoked when a [FinancialConnectionsSheetForTokenResult] is available.
 */
fun interface FinancialConnectionsSheetResultForLinkCallback {
    fun onFinancialConnectionsSheetResult(
        financialConnectionsSheetForLinkResult: FinancialConnectionsSheetLinkResult
    )
}
