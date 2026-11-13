package com.stripe.android.checkout.analytics

import com.stripe.android.checkout.analytics.CheckoutAnalyticsEvent.Operation
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject

/** Reports the started and terminal outcomes of a Checkout operation around [measure]'s block. */
internal class CheckoutOperationMeasurer @Inject constructor(
    private val analyticsReporter: CheckoutAnalyticsReporter,
) {
    suspend fun measure(
        operation: Operation,
        checkoutSessionResponse: CheckoutSessionResponse?,
        params: Map<String, Any?>,
        block: suspend () -> Result<CheckoutSessionResponse>,
    ): Result<CheckoutSessionResponse> {
        val measuredOperation = MeasuredOperation(
            operation = operation,
            params = params,
            report = analyticsReporter::fireEvent,
        )
        measuredOperation.started(checkoutSessionResponse)
        return block()
            .onSuccess { measuredOperation.succeeded(it) }
            .onFailure { measuredOperation.failed(checkoutSessionResponse, it) }
    }
}
