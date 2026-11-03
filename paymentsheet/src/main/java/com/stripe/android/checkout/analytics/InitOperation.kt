@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout.analytics

import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Operation
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Outcome
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlin.time.Duration
import kotlin.time.TimeSource

internal class InitOperation(
    clientSecret: String,
    configuration: CheckoutController.Configuration.State,
    timeSource: TimeSource,
    report: (CheckoutAnalyticsEvent) -> Unit,
) : CheckoutAnalyticsOperation<Throwable>(Operation.Init, report) {
    var response: CheckoutSessionResponse? = null
        private set

    var orderedLpms: List<String> = emptyList()

    // Configure attempts can overlap while queued, so each owns its start mark.
    private val startedAt = timeSource.markNow()

    override val params: Map<String, Any?> = buildMap {
        // Without the delimiter, substringBefore would expose the whole client secret.
        if ("_secret_" in clientSecret) {
            put("checkout_session_id", clientSecret.substringBefore("_secret_"))
        }
        putAll(configuration.toAnalyticsMap())
    }

    fun recordResponse(response: CheckoutSessionResponse) {
        this.response = response
    }

    override fun outcomeParams(outcome: Outcome): Map<String, Any?> = if (outcome == Outcome.Succeeded) {
        params + mapOf("ordered_lpms" to orderedLpms.joinToString(","))
    } else {
        params
    }

    override fun elapsed(): Duration = startedAt.elapsedNow()

    override fun analyticsError(error: Throwable): CheckoutAnalyticsEvent.Error =
        CheckoutAnalyticsEvent.Error.from(error)
}
