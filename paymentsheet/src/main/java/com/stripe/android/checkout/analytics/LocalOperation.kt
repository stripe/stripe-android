package com.stripe.android.checkout.analytics

import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Operation
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlin.time.Duration

internal class LocalOperation(
    operation: Operation,
    override val params: Map<String, Any?>,
    report: (CheckoutAnalyticsEvent) -> Unit,
) : CheckoutAnalyticsOperation<Nothing>(operation, report) {
    fun invoked(response: CheckoutSessionResponse?) {
        reportOutcome(outcome = null, response = response, error = null)
    }

    override fun elapsed(): Duration? = null

    override fun analyticsError(error: Nothing): CheckoutAnalyticsEvent.Error = error
}
