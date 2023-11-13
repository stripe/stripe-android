package com.stripe.android.utils

import com.stripe.android.model.ElementsSession
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository

internal class FakeElementsSessionRepository(
    private val stripeIntent: StripeIntent,
    private val error: Throwable?,
    private val fallbackError: Throwable? = null,
    private val linkSettings: ElementsSession.LinkSettings?,
    private val isGooglePayEnabled: Boolean = true,
) : ElementsSessionRepository {

    var lastGetParam: PaymentSheet.InitializationMode? = null

    override suspend fun get(
        initializationMode: PaymentSheet.InitializationMode,
    ): Result<ElementsSessionRepository.LoadResult> {
        lastGetParam = initializationMode
        return if (error != null) {
            Result.failure(error)
        } else {
            val loadResult = if (fallbackError != null) {
                ElementsSessionRepository.LoadResult.Fallback(
                    stripeIntent = stripeIntent,
                    error = fallbackError,
                )
            } else {
                ElementsSessionRepository.LoadResult.Session(
                    ElementsSession(
                        linkSettings = linkSettings,
                        paymentMethodSpecs = null,
                        stripeIntent = stripeIntent,
                        merchantCountry = null,
                        isEligibleForCardBrandChoice = true,
                        isGooglePayEnabled = isGooglePayEnabled,
                    )
                )
            }
            Result.success(loadResult)
        }
    }
}
