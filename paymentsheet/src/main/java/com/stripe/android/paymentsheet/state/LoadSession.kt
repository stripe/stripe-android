package com.stripe.android.paymentsheet.state

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.model.ElementsSession
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import javax.inject.Inject

internal data class SessionResult(
    val elementsSession: ElementsSession,
    val checkoutSession: CheckoutSessionResponse?,
)

internal interface LoadSession {
    suspend operator fun invoke(
        initializationMode: PaymentElementLoader.InitializationMode,
        configuration: CommonConfiguration,
        savedPaymentMethodSelection: SavedSelection.PaymentMethod?,
    ): SessionResult
}

/**
 * Dispatches to the appropriate strategy based on initialization mode:
 * - [CheckoutSessionLoader] for checkout session flows
 * - [ElementsSessionLoader] for all other flows
 */
internal class DefaultLoadSession @Inject constructor(
    private val checkoutSessionLoader: CheckoutSessionLoader,
    private val elementsSessionLoader: ElementsSessionLoader,
) : LoadSession {

    override suspend fun invoke(
        initializationMode: PaymentElementLoader.InitializationMode,
        configuration: CommonConfiguration,
        savedPaymentMethodSelection: SavedSelection.PaymentMethod?,
    ): SessionResult {
        return if (initializationMode is PaymentElementLoader.InitializationMode.CheckoutSession) {
            checkoutSessionLoader(initializationMode)
        } else {
            elementsSessionLoader(
                initializationMode = initializationMode,
                configuration = configuration,
                savedPaymentMethodSelection = savedPaymentMethodSelection,
            )
        }
    }
}

/**
 * Loads session data via [CheckoutSessionRepository].
 *
 * The checkout session init response contains the elements session and customer data.
 */
internal class CheckoutSessionLoader @Inject constructor(
    private val checkoutSessionRepository: CheckoutSessionRepository,
) {
    suspend operator fun invoke(
        initializationMode: PaymentElementLoader.InitializationMode.CheckoutSession,
    ): SessionResult {
        val checkoutSession = checkoutSessionRepository.init(
            sessionId = initializationMode.id,
        ).getOrThrow()

        val elementsSession = checkoutSession.elementsSession
            ?: throw IllegalStateException("CheckoutSession init response missing elements_session")

        return SessionResult(
            elementsSession = elementsSession,
            checkoutSession = checkoutSession,
        )
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
    ): SessionResult {
        val elementsSession = elementsSessionRepository.get(
            initializationMode = initializationMode,
            customer = configuration.customer,
            externalPaymentMethods = configuration.externalPaymentMethods,
            customPaymentMethods = configuration.customPaymentMethods,
            savedPaymentMethodSelectionId = savedPaymentMethodSelection?.id,
            countryOverride = configuration.userOverrideCountry,
            linkDisallowedFundingSourceCreation = configuration.link.disallowFundingSourceCreation,
        ).getOrThrow()

        return SessionResult(
            elementsSession = elementsSession,
            checkoutSession = null,
        )
    }
}
