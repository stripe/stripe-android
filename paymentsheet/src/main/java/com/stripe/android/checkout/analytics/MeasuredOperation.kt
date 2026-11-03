package com.stripe.android.checkout.analytics

import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Operation
import kotlin.time.Duration
import kotlin.time.TimeSource

/** Reports a Checkout operation that starts and finishes within a single call. */
internal class MeasuredOperation(
    operation: Operation,
    override val params: Map<String, Any?>,
    report: (CheckoutAnalyticsEvent) -> Unit,
) : CheckoutAnalyticsOperation<Throwable>(operation, report) {
    private val startedAt = TimeSource.Monotonic.markNow()

    override fun elapsed(): Duration = startedAt.elapsedNow()

    override fun analyticsError(error: Throwable): CheckoutAnalyticsEvent.Error =
        CheckoutAnalyticsEvent.Error.from(error)
}
