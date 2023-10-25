package com.stripe.android.utils

import com.stripe.android.model.ElementsSession
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository

internal class FakeElementsSessionRepository(
    private val stripeIntent: StripeIntent,
    private val error: Throwable?,
    private val sessionsError: Throwable? = null,
    private val linkSettings: ElementsSession.LinkSettings?,
    private val isGooglePayEnabled: Boolean = true,
    private val isCbcEligible: Boolean = false,
) : ElementsSessionRepository {

    var lastGetParam: PaymentSheet.InitializationMode? = null

    override suspend fun get(
        initializationMode: PaymentSheet.InitializationMode,
    ): Result<ElementsSession> {
        lastGetParam = initializationMode
        return if (error != null) {
            Result.failure(error)
        } else {
            Result.success(
                ElementsSession(
                    linkSettings = linkSettings,
                    paymentMethodSpecs = null,
                    stripeIntent = stripeIntent,
                    merchantCountry = null,
                    isEligibleForCardBrandChoice = isCbcEligible,
                    isGooglePayEnabled = isGooglePayEnabled,
                    sessionsError = sessionsError,
                )
            )
        }
    }
}
