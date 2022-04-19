package com.stripe.android.financialconnections.analytics

import com.stripe.android.financialconnections.ConnectionsSheet
import com.stripe.android.financialconnections.ConnectionsSheetResult

/**
 * Contract for connections related analytic events that will be tracked.
 *
 * Implementation will encapsulate the firing event logic.
 *
 * @see [ConnectionsAnalyticsEvent].
 *
 */
internal interface ConnectionsEventReporter {

    fun onPresented(configuration: ConnectionsSheet.Configuration)

    fun onResult(
        configuration: ConnectionsSheet.Configuration,
        connectionsSheetResult: ConnectionsSheetResult
    )
}
