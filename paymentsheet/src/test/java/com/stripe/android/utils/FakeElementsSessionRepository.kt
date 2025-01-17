package com.stripe.android.utils

import com.stripe.android.model.ElementsSession
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.paymentsheet.state.PaymentElementLoader

internal class FakeElementsSessionRepository(
    private val stripeIntent: StripeIntent,
    private val error: Throwable?,
    private val sessionsError: Throwable? = null,
    private val linkSettings: ElementsSession.LinkSettings?,
    private val sessionsCustomer: ElementsSession.Customer? = null,
    private val isGooglePayEnabled: Boolean = true,
    private val cardBrandChoice: ElementsSession.CardBrandChoice? = null,
    private val externalPaymentMethodData: String? = null,
) : ElementsSessionRepository {
    data class Params(
        val initializationMode: PaymentElementLoader.InitializationMode,
        val customer: PaymentSheet.CustomerConfiguration?,
        val externalPaymentMethods: List<String>,
        val savedPaymentMethodSelectionId: String?
    )

    var lastParams: Params? = null

    override suspend fun get(
        initializationMode: PaymentElementLoader.InitializationMode,
        customer: PaymentSheet.CustomerConfiguration?,
        externalPaymentMethods: List<String>,
        savedPaymentMethodSelectionId: String?,
    ): Result<ElementsSession> {
        lastParams = Params(
            initializationMode = initializationMode,
            customer = customer,
            externalPaymentMethods = externalPaymentMethods,
            savedPaymentMethodSelectionId = savedPaymentMethodSelectionId,
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
                    isGooglePayEnabled = isGooglePayEnabled,
                    sessionsError = sessionsError,
                    externalPaymentMethodData = externalPaymentMethodData,
                    customer = sessionsCustomer,
                    cardBrandChoice = cardBrandChoice,
                )
            )
        }
    }
}
