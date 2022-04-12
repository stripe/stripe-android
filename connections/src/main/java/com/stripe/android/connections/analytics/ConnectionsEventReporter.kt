package com.stripe.android.connections.analytics

import com.stripe.android.connections.ConnectionsSheet
import com.stripe.android.connections.launcher.ConnectionsSheetContract

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
        connectionsSheetResult: ConnectionsSheetContract.Result
    )
}
