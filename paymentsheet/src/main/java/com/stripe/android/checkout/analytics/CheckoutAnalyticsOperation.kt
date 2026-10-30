package com.stripe.android.checkout.analytics

import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Operation
import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Outcome
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.state.DefaultAnalyticsMetadataFactory
import kotlin.time.Duration

/**
 * Reports the outcomes of a Checkout operation, with [E] as its failure input. Subclasses own the
 * success payload because it varies by operation.
 */
internal abstract class CheckoutAnalyticsOperation<E>(
    private val operation: Operation,
    private val sessionId: String?,
    private val report: (CheckoutAnalyticsEvent) -> Unit,
) {
    protected abstract val params: Map<String, Any?>

    /** Called once, on the terminal outcome; may end a shared timer. */
    protected abstract fun elapsed(): Duration?

    protected abstract fun analyticsError(error: E): CheckoutAnalyticsEvent.Error

    fun started(response: CheckoutSessionResponse?) {
        reportOutcome(Outcome.Started, response, additionalParams = emptyMap(), error = null)
    }

    fun failed(response: CheckoutSessionResponse?, error: E) {
        reportOutcome(Outcome.Failed, response, additionalParams = emptyMap(), error = analyticsError(error))
    }

    protected fun reportOutcome(
        outcome: Outcome,
        response: CheckoutSessionResponse?,
        additionalParams: Map<String, Any?>,
        error: CheckoutAnalyticsEvent.Error?,
    ) {
        report(
            CheckoutAnalyticsEvent(
                operation = operation,
                outcome = outcome,
                sessionId = sessionId,
                analyticsMetadata = DefaultAnalyticsMetadataFactory.create(response),
                duration = if (outcome == Outcome.Started) null else elapsed(),
                error = error,
                operationParams = params + additionalParams,
            )
        )
    }
}
