package com.stripe.android.utils

import com.stripe.android.model.ElementsSession
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository

internal class FakeElementsSessionRepository(
    private val stripeIntent: StripeIntent,
    private val error: Throwable?,
) : ElementsSessionRepository {

    override suspend fun get(
        initializationMode: PaymentSheet.InitializationMode,
    ): Result<ElementsSession> {
        return if (error != null) {
            Result.failure(error)
        } else {
            Result.success(
                ElementsSession(
                    linkSettings = null,
                    paymentMethodSpecs = null,
                    stripeIntent = stripeIntent,
                    merchantCountry = null,
                )
            )
        }
    }
}
