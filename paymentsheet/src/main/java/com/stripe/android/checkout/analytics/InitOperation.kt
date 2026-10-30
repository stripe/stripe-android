package com.stripe.android.checkout.analytics

import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Operation
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Outcome
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.coroutines.CancellationException
import kotlin.time.Duration
import kotlin.time.TimeMark

internal class InitOperation(
    sessionId: String?,
    private val startedAt: TimeMark,
    report: (CheckoutAnalyticsEvent) -> Unit,
) : CheckoutAnalyticsOperation<Throwable>(Operation.Init, sessionId, report) {
    override val params: Map<String, Any?> = emptyMap()
    private var response: CheckoutSessionResponse? = null

    fun updateResponse(response: CheckoutSessionResponse) {
        this.response = response
    }

    suspend fun perform(block: suspend () -> Result<List<String>>): Result<List<String>> {
        started(response = null)
        return block()
            .onSuccess { succeeded(response, it) }
            .onFailure {
                if (it is CancellationException) throw it
                failed(response, it)
            }
    }

    // Configure calls can overlap, so each attempt keeps its own start mark.
    override fun elapsed(): Duration = startedAt.elapsedNow()

    override fun analyticsError(error: Throwable): CheckoutAnalyticsEvent.Error =
        CheckoutAnalyticsEvent.Error.from(error)

    fun succeeded(response: CheckoutSessionResponse?, orderedLpms: List<String>) {
        reportOutcome(
            outcome = Outcome.Succeeded,
            response = response,
            additionalParams = mapOf("ordered_lpms" to orderedLpms.joinToString(",")),
            error = null,
        )
    }
}
