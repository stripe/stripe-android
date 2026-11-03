package com.stripe.android.checkout.analytics

import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.coroutines.CancellationException

internal suspend fun <T> CheckoutAnalyticsOperation<Throwable>.measure(
    startingResponse: CheckoutSessionResponse?,
    response: () -> CheckoutSessionResponse?,
    block: suspend () -> Result<T>,
): Result<T> {
    started(startingResponse)
    val result = try {
        block()
    } catch (error: CancellationException) {
        // Cancellation while queued throws instead of returning a failure.
        failed(response(), error)
        throw error
    }
    return result
        .onSuccess { succeeded(response()) }
        .onFailure { failed(response(), it) }
}
