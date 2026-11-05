package com.stripe.android.checkout.analytics

import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Operation
import kotlin.time.Duration

internal class LocalOperation(
    operation: Operation,
    report: (CheckoutAnalyticsEvent) -> Unit,
) : CheckoutAnalyticsOperation<Nothing>(operation, report) {
    override val params: Map<String, Any?> = emptyMap()

    override fun elapsed(): Duration? = null

    override fun analyticsError(error: Nothing): CheckoutAnalyticsEvent.Error = error
}
