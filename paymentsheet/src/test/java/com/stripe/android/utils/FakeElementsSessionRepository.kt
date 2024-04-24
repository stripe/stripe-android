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
    private val customer: ElementsSession.Customer? = null,
    private val isGooglePayEnabled: Boolean = true,
    private val isCbcEligible: Boolean = false,
    private val externalPaymentMethodData: String? = null,
) : ElementsSessionRepository {
    data class Params(
        val initializationMode: PaymentSheet.InitializationMode,
        val customer: PaymentSheet.CustomerConfiguration?,
        val externalPaymentMethods: List<String>?,
    )

    var lastParams: Params? = null

    override suspend fun get(
        initializationMode: PaymentSheet.InitializationMode,
        customer: PaymentSheet.CustomerConfiguration?,
        externalPaymentMethods: List<String>?,
    ): Result<ElementsSession> {
        lastParams = Params(
            initializationMode = initializationMode,
            customer = customer,
            externalPaymentMethods = externalPaymentMethods
        )
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
                    externalPaymentMethodData = externalPaymentMethodData,
                    customer = this.customer,
                )
            )
        }
    }
}
