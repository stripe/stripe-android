package com.stripe.android.checkout

import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import kotlinx.coroutines.CancellationException

internal data class TaxRegionUpdateOutcome(
    val result: Result<CheckoutSessionResponse>,
    val requestAttempted: Boolean,
) {
    fun resultOrThrowCancellation(): Result<CheckoutSessionResponse> {
        result.exceptionOrNull()?.let { error ->
            if (error is CancellationException) throw error
        }
        return result
    }
}
