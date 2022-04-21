package com.stripe.android.financialconnections.analytics

import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class DefaultFinancialFinancialConnectionsEventReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    @IOContext private val workContext: CoroutineContext
) : FinancialConnectionsEventReporter {

    override fun onPresented(configuration: FinancialConnectionsSheet.Configuration) {
        fireEvent(
            FinancialConnectionsAnalyticsEvent(
                FinancialConnectionsAnalyticsEvent.Code.SheetPresented,
                mapOf(PARAM_CLIENT_SECRET to configuration.linkAccountSessionClientSecret)
            )
        )
    }

    override fun onResult(
        configuration: FinancialConnectionsSheet.Configuration,
        financialConnectionsSheetResult: FinancialConnectionsSheetContract.Result
    ) {
        val event = when (financialConnectionsSheetResult) {
            is FinancialConnectionsSheetContract.Result.Completed ->
                FinancialConnectionsAnalyticsEvent(
                    FinancialConnectionsAnalyticsEvent.Code.SheetClosed,
                    mapOf(
                        PARAM_CLIENT_SECRET to configuration.linkAccountSessionClientSecret,
                        PARAM_SESSION_RESULT to "completed"
                    )
                )
            is FinancialConnectionsSheetContract.Result.Canceled ->
                FinancialConnectionsAnalyticsEvent(
                    FinancialConnectionsAnalyticsEvent.Code.SheetClosed,
                    mapOf(
                        PARAM_CLIENT_SECRET to configuration.linkAccountSessionClientSecret,
                        PARAM_SESSION_RESULT to "cancelled"
                    )
                )
            is FinancialConnectionsSheetContract.Result.Failed ->
                FinancialConnectionsAnalyticsEvent(
                    FinancialConnectionsAnalyticsEvent.Code.SheetFailed,
                    mapOf(
                        PARAM_CLIENT_SECRET to configuration.linkAccountSessionClientSecret,
                        PARAM_SESSION_RESULT to "failure"
                    )
                )
        }

        fireEvent(event)
    }

    private fun fireEvent(event: FinancialConnectionsAnalyticsEvent) {
        CoroutineScope(workContext).launch {
            analyticsRequestExecutor.executeAsync(
                analyticsRequestFactory.createRequest(
                    event = event,
                    additionalParams = event.additionalParams
                )
            )
        }
    }

    internal companion object {
        const val PARAM_CLIENT_SECRET = "las_client_secret"
        const val PARAM_SESSION_RESULT = "session_result"
    }
}
