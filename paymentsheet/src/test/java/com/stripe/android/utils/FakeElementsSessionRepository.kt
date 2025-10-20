package com.stripe.android.utils

import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PassiveCaptchaParams
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
    private val customPaymentMethods: List<ElementsSession.CustomPaymentMethod> = emptyList(),
    private val cardBrandChoice: ElementsSession.CardBrandChoice? = null,
    private val externalPaymentMethodData: String? = null,
    private val passiveCaptchaParams: PassiveCaptchaParams? = null
) : ElementsSessionRepository {
    data class Params(
        val initializationMode: PaymentElementLoader.InitializationMode,
        val customer: PaymentSheet.CustomerConfiguration?,
        val customPaymentMethods: List<PaymentSheet.CustomPaymentMethod>,
        val externalPaymentMethods: List<String>,
        val savedPaymentMethodSelectionId: String?,
        val userOverrideCountry: String?,
        val linkDisallowedFundingSourceCreation: Set<String>,
    )

    var lastParams: Params? = null

    override suspend fun get(
        initializationMode: PaymentElementLoader.InitializationMode,
        customer: PaymentSheet.CustomerConfiguration?,
        customPaymentMethods: List<PaymentSheet.CustomPaymentMethod>,
        externalPaymentMethods: List<String>,
        savedPaymentMethodSelectionId: String?,
        userOverrideCountry: String?,
        linkDisallowedFundingSourceCreation: Set<String>,
    ): Result<ElementsSession> {
        lastParams = Params(
            initializationMode = initializationMode,
            customer = customer,
            externalPaymentMethods = externalPaymentMethods,
            customPaymentMethods = customPaymentMethods,
            savedPaymentMethodSelectionId = savedPaymentMethodSelectionId,
            userOverrideCountry = userOverrideCountry,
            linkDisallowedFundingSourceCreation = linkDisallowedFundingSourceCreation,
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
                    customPaymentMethods = this.customPaymentMethods,
                    elementsSessionId = DEFAULT_ELEMENTS_SESSION_ID,
                    flags = mapOf(
                        ElementsSession.Flag.ELEMENTS_ENABLE_PASSIVE_CAPTCHA to true
                    ),
                    orderedPaymentMethodTypesAndWallets = stripeIntent.paymentMethodTypes,
                    experimentsData = null,
                    passiveCaptcha = passiveCaptchaParams,
                    merchantLogoUrl = null,
                    elementsSessionConfigId = DEFAULT_ELEMENTS_SESSION_CONFIG_ID,
                )
            )
        }
    }

    companion object {
        const val DEFAULT_ELEMENTS_SESSION_ID = "session_1234"
        const val DEFAULT_ELEMENTS_SESSION_CONFIG_ID = "config_1234"
    }
}
