package com.stripe.android.checkout

import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import javax.inject.Inject

internal class CheckoutSessionLoader @Inject internal constructor(
    private val repository: CheckoutSessionRepository,
) {
    suspend fun load(checkoutSessionClientSecret: String): Result<CheckoutSessionResponse> {
        val sessionId = checkoutSessionClientSecret.substringBefore("_secret_")
        return repository.init(
            sessionId = sessionId,
        )
    }
}
