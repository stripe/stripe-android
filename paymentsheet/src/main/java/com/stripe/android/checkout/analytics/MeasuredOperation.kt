package com.stripe.android.checkout.analytics

import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Operation
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Reports a Checkout operation that starts and finishes within a single call. */
internal class MeasuredOperation(
    operation: Operation,
    override val params: Map<String, Any?>,
    report: (CheckoutAnalyticsEvent) -> Unit,
) : CheckoutAnalyticsOperation<Throwable>(operation, report) {
    private var startedAt: TimeMark? = null

    override fun started(response: CheckoutSessionResponse?) {
        startedAt = TimeSource.Monotonic.markNow()
        super.started(response)
    }

    override fun elapsed(): Duration? = startedAt?.elapsedNow()

    override fun analyticsError(error: Throwable): CheckoutAnalyticsEvent.Error =
        CheckoutAnalyticsEvent.Error.from(error)
}
