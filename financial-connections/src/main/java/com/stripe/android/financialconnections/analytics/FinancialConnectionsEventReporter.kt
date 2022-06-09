package com.stripe.android.financialconnections.analytics

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult

/**
 * Contract for connections related analytic events that will be tracked.
 *
 * Implementation will encapsulate the firing event logic.
 *
 * @see [FinancialConnectionsAnalyticsEvent].
 *
 */
internal interface FinancialConnectionsEventReporter {

    fun onPresented(configuration: FinancialConnectionsSheet.Configuration)

    fun onResult(
        configuration: FinancialConnectionsSheet.Configuration,
        financialConnectionsSheetResult: FinancialConnectionsSheetActivityResult
    )
}
