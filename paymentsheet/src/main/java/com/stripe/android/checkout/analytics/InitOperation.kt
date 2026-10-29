package com.stripe.android.checkout.analytics

import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Operation
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Outcome
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlin.time.Duration
import kotlin.time.TimeMark

internal class InitOperation(
    sessionId: String?,
    private val startedAt: TimeMark,
    report: (CheckoutAnalyticsEvent) -> Unit,
) : CheckoutAnalyticsOperation<Throwable>(Operation.Init, sessionId, report) {
    override val params: Map<String, Any?> = emptyMap()

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
