package com.stripe.android.utils

import com.stripe.android.model.ElementsSession
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository

internal class FakeElementsSessionRepository(
    private val stripeIntent: StripeIntent?,
) : ElementsSessionRepository {

    override suspend fun get(
        initializationMode: PaymentSheet.InitializationMode,
    ): Result<ElementsSession> {
        return if (stripeIntent != null) {
            Result.success(
                ElementsSession(
                    linkSettings = null,
                    paymentMethodSpecs = null,
                    stripeIntent = stripeIntent,
                    merchantCountry = null,
                )
            )
        } else {
            Result.failure(RuntimeException())
        }
    }
}
