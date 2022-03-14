package com.stripe.android.connections.analytics

import com.stripe.android.connections.ConnectionsSheet
import com.stripe.android.connections.ConnectionsSheetResult
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class DefaultConnectionsEventReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val connectionsAnalyticsRequestFactory: ConnectionsAnalyticsRequestFactory,
    @IOContext private val workContext: CoroutineContext
) : ConnectionsEventReporter {

    override fun onPresented(configuration: ConnectionsSheet.Configuration) {
        fireEvent(
            ConnectionsAnalyticsEvent(
                ConnectionsAnalyticsEvent.Code.SheetPresented,
                mapOf(PARAM_CLIENT_SECRET to configuration.linkAccountSessionClientSecret)
            )
        )
    }

    override fun onResult(
        configuration: ConnectionsSheet.Configuration,
        connectionsSheetResult: ConnectionsSheetResult
    ) {
        val event = when (connectionsSheetResult) {
            is ConnectionsSheetResult.Completed ->
                ConnectionsAnalyticsEvent(
                    ConnectionsAnalyticsEvent.Code.SheetClosed,
                    mapOf(
                        PARAM_CLIENT_SECRET to configuration.linkAccountSessionClientSecret,
                        PARAM_SESSION_RESULT to "completed"
                    )
                )
            is ConnectionsSheetResult.Canceled ->
                ConnectionsAnalyticsEvent(
                    ConnectionsAnalyticsEvent.Code.SheetClosed,
                    mapOf(
                        PARAM_CLIENT_SECRET to configuration.linkAccountSessionClientSecret,
                        PARAM_SESSION_RESULT to "cancelled"
                    )
                )
            is ConnectionsSheetResult.Failed ->
                ConnectionsAnalyticsEvent(
                    ConnectionsAnalyticsEvent.Code.SheetFailed,
                    mapOf(
                        PARAM_CLIENT_SECRET to configuration.linkAccountSessionClientSecret,
                        PARAM_SESSION_RESULT to "failure"
                    )
                )
        }

        fireEvent(event)
    }

    private fun fireEvent(event: ConnectionsAnalyticsEvent) {
        CoroutineScope(workContext).launch {
            analyticsRequestExecutor.executeAsync(
                connectionsAnalyticsRequestFactory.createRequest(event)
            )
        }
    }

    internal companion object {
        const val PARAM_CLIENT_SECRET = "las_client_secret"
        const val PARAM_SESSION_RESULT = "session_result"
    }
}
