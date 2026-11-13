package com.stripe.android.paymentsheet.state

import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.model.ElementsSession
import com.stripe.android.payments.financialconnections.DefaultIsFinancialConnectionsAvailable
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.toMobileSessionConfig
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import javax.inject.Inject

/**
 * Extracts session data from the [CheckoutSessionResponse] already loaded during
 * [CheckoutElementsController.configure].
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
        configuration: PaymentElementLoader.Configuration,
        savedPaymentMethodSelection: SavedSelection.PaymentMethod?,
        apiConfiguration: ApiConfiguration.State,
    ): ElementsSession {
        val commonConfiguration = configuration.commonConfiguration
        val sendsMobileSessionConfig = when (configuration) {
            is PaymentElementLoader.Configuration.PaymentSheet,
            is PaymentElementLoader.Configuration.Embedded -> true
            is PaymentElementLoader.Configuration.CryptoOnramp,
            is PaymentElementLoader.Configuration.StandaloneLink,
            is PaymentElementLoader.Configuration.ExpressCheckoutElement -> false
        }
        val mobileSessionConfig = if (
            sendsMobileSessionConfig && FeatureFlags.serverPaymentMethodAvailabilityEnabled.isEnabled
        ) {
            commonConfiguration.toMobileSessionConfig(
                hasFinancialConnectionsSdk = DefaultIsFinancialConnectionsAvailable() &&
                    !FeatureFlags.financialConnectionsFullSdkUnavailable.isEnabled,
            )
        } else {
            null
        }

        return elementsSessionRepository.get(
            initializationMode = initializationMode,
            customer = commonConfiguration.customer,
            externalPaymentMethods = commonConfiguration.externalPaymentMethods,
            customPaymentMethods = commonConfiguration.customPaymentMethods,
            savedPaymentMethodSelectionId = savedPaymentMethodSelection?.id,
            countryOverride = commonConfiguration.userOverrideCountry,
            apiConfiguration = apiConfiguration,
            mobileSessionConfig = mobileSessionConfig,
            linkDisallowedFundingSourceCreation = commonConfiguration.link.disallowFundingSourceCreation,
        ).getOrThrow()
    }
}
