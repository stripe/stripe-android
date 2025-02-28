package com.stripe.android.financialconnections.analytics

import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult
import com.stripe.android.financialconnections.utils.filterNotNullValues
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class DefaultFinancialConnectionsEventReporter @Inject constructor(
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    @IOContext private val workContext: CoroutineContext
) : FinancialConnectionsEventReporter {

    override fun onPresented() {
        fireEvent(Event(Event.Code.SheetPresented))
    }

    override fun onResult(
        sessionId: String,
        financialConnectionsSheetResult: FinancialConnectionsSheetActivityResult
    ) {
        val event = when (financialConnectionsSheetResult) {
            is FinancialConnectionsSheetActivityResult.Completed ->
                Event(
                    Event.Code.SheetClosed,
                    mapOf(
                        PARAM_SESSION_ID to sessionId,
                        PARAM_SESSION_RESULT to "completed"
                    )
                )

            is FinancialConnectionsSheetActivityResult.Canceled ->
                Event(
                    Event.Code.SheetClosed,
                    mapOf(
                        PARAM_SESSION_ID to sessionId,
                        PARAM_SESSION_RESULT to "cancelled"
                    )
                )

            is FinancialConnectionsSheetActivityResult.Failed ->
                Event(
                    Event.Code.SheetFailed,
                    mapOf(
                        PARAM_SESSION_ID to sessionId,
                        PARAM_SESSION_RESULT to "failure"
                    ).plus(
                        financialConnectionsSheetResult.error
                            .toEventParams(extraMessage = null)
                            .filterNotNullValues()
                    )
                )
        }

        fireEvent(event)
    }

    private fun fireEvent(event: Event) {
        CoroutineScope(workContext).launch {
            analyticsRequestExecutor.executeAsync(
                analyticsRequestFactory.createRequest(
                    event = event,
                    additionalParams = event.additionalParams
                )
            )
        }
    }

    private data class Event(
        val eventCode: Code,
        val additionalParams: Map<String, String> = emptyMap()
    ) : AnalyticsEvent {

        override val eventName: String = eventCode.toString()

        enum class Code(internal val code: String) {

            SheetPresented("sheet.presented"),
            SheetClosed("sheet.closed"),
            SheetFailed("sheet.failed");

            override fun toString(): String {
                return "$PREFIX.$code"
            }

            private companion object {
                private const val PREFIX = "stripe_android.connections"
            }
        }
    }

    internal companion object {
        const val PARAM_SESSION_ID = "las_id"
        const val PARAM_SESSION_RESULT = "session_result"
    }
}
