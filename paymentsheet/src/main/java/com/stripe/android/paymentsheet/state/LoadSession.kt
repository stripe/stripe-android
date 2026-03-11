package com.stripe.android.paymentsheet.state

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.model.ElementsSession
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import javax.inject.Inject

/**
 * Extracts session data from the [CheckoutSessionResponse] already loaded during [Checkout.configure].
 *
 * The checkout session init response contains the elements session and customer data.
 */
internal class CheckoutSessionLoader @Inject constructor() {
    operator fun invoke(
        initializationMode: PaymentElementLoader.InitializationMode.CheckoutSession,
    ): ElementsSession {
        return initializationMode.checkoutSessionResponse.elementsSession
            ?: throw IllegalStateException("CheckoutSession init response missing elements_session")
    }
}

/**
 * Loads session data via [ElementsSessionRepository] for standard (non-checkout) flows.
 */
internal class ElementsSessionLoader @Inject constructor(
    private val elementsSessionRepository: ElementsSessionRepository,
) {
    suspend operator fun invoke(
        initializationMode: PaymentElementLoader.InitializationMode,
        configuration: CommonConfiguration,
        savedPaymentMethodSelection: SavedSelection.PaymentMethod?,
    ): ElementsSession {
        return elementsSessionRepository.get(
            initializationMode = initializationMode,
            customer = configuration.customer,
            externalPaymentMethods = configuration.externalPaymentMethods,
            customPaymentMethods = configuration.customPaymentMethods,
            savedPaymentMethodSelectionId = savedPaymentMethodSelection?.id,
            countryOverride = configuration.userOverrideCountry,
            linkDisallowedFundingSourceCreation = configuration.link.disallowFundingSourceCreation,
        ).getOrThrow()
    }
}
