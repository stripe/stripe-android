package com.stripe.android.paymentsheet.state

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.core.exception.StripeException
import com.stripe.android.model.ElementsSession
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import javax.inject.Inject

internal data class SessionAndCustomerInfo(
    val elementsSession: ElementsSession,
    val checkoutSession: CheckoutSessionResponse?,
    val customerInfo: CustomerInfo?,
)

internal interface LoadSessionAndCustomerInfo {
    suspend operator fun invoke(
        initializationMode: PaymentElementLoader.InitializationMode,
        configuration: CommonConfiguration,
        savedPaymentMethodSelection: SavedSelection.PaymentMethod?,
    ): SessionAndCustomerInfo
}

internal class DefaultLoadSessionAndCustomerInfo @Inject constructor(
    private val checkoutSessionRepository: CheckoutSessionRepository,
    private val elementsSessionRepository: ElementsSessionRepository,
    private val errorReporter: ErrorReporter,
) : LoadSessionAndCustomerInfo {

    override suspend fun invoke(
        initializationMode: PaymentElementLoader.InitializationMode,
        configuration: CommonConfiguration,
        savedPaymentMethodSelection: SavedSelection.PaymentMethod?,
    ): SessionAndCustomerInfo {
        val checkoutSession = retrieveCheckoutSession(initializationMode)
        val elementsSession = retrieveElementsSession(
            initializationMode = initializationMode,
            checkoutSession = checkoutSession,
            configuration = configuration,
            savedPaymentMethodSelection = savedPaymentMethodSelection,
        )
        val customerInfo = createCustomerInfo(
            configuration = configuration,
            elementsSession = elementsSession,
            checkoutSession = checkoutSession,
        )
        return SessionAndCustomerInfo(
            elementsSession = elementsSession,
            checkoutSession = checkoutSession,
            customerInfo = customerInfo,
        )
    }

    /**
     * Fetches the [CheckoutSessionResponse] if the initialization mode is a checkout session.
     * Returns null for all other flows.
     */
    private suspend fun retrieveCheckoutSession(
        initializationMode: PaymentElementLoader.InitializationMode,
    ): CheckoutSessionResponse? {
        if (initializationMode !is PaymentElementLoader.InitializationMode.CheckoutSession) {
            return null
        }
        return checkoutSessionRepository.init(
            sessionId = initializationMode.id,
        ).getOrThrow()
    }

    /**
     * Returns the [ElementsSession] — extracted from the checkout session response if present,
     * or fetched via the standard elements session repository otherwise.
     */
    private suspend fun retrieveElementsSession(
        initializationMode: PaymentElementLoader.InitializationMode,
        checkoutSession: CheckoutSessionResponse?,
        configuration: CommonConfiguration,
        savedPaymentMethodSelection: SavedSelection.PaymentMethod?,
    ): ElementsSession {
        if (checkoutSession != null) {
            return checkoutSession.elementsSession
                ?: throw IllegalStateException("CheckoutSession init response missing elements_session")
        }
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

    private fun createCustomerInfo(
        configuration: CommonConfiguration,
        elementsSession: ElementsSession,
        checkoutSession: CheckoutSessionResponse?,
    ): CustomerInfo? {
        // Checkout session customer data comes from the init response, not from customer sessions.
        val checkoutCustomer = checkoutSession?.customer
        if (checkoutCustomer != null) {
            return CustomerInfo.CheckoutSession(
                customer = checkoutCustomer,
                offerSave = checkoutSession.savedPaymentMethodsOfferSave,
            )
        }

        val customer = configuration.customer

        return when (val accessType = customer?.accessType) {
            is PaymentSheet.CustomerAccessType.CustomerSession -> {
                elementsSession.customer?.let { elementsSessionCustomer ->
                    CustomerInfo.CustomerSession(elementsSessionCustomer, accessType.customerSessionClientSecret)
                } ?: run {
                    val exception = IllegalStateException(
                        "Excepted 'customer' attribute as part of 'elements_session' response!"
                    )

                    errorReporter.report(
                        ErrorReporter.UnexpectedErrorEvent.PAYMENT_SHEET_LOADER_ELEMENTS_SESSION_CUSTOMER_NOT_FOUND,
                        StripeException.create(exception)
                    )

                    if (!elementsSession.stripeIntent.isLiveMode) {
                        throw exception
                    }

                    null
                }
            }
            is PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey -> {
                CustomerInfo.Legacy(
                    customerConfig = customer,
                    accessType = accessType,
                )
            }
            else -> null
        }
    }
}

internal sealed interface CustomerInfo {
    data class CustomerSession(
        val elementsSessionCustomer: ElementsSession.Customer,
        val customerSessionClientSecret: String,
    ) : CustomerInfo {
        val id: String = elementsSessionCustomer.session.customerId
        val ephemeralKeySecret: String = elementsSessionCustomer.session.apiKey
    }

    data class Legacy(
        val customerConfig: PaymentSheet.CustomerConfiguration,
        val accessType: PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey
    ) : CustomerInfo {
        val id: String = customerConfig.id
        val ephemeralKeySecret: String = accessType.ephemeralKeySecret
    }

    /**
     * Customer data from checkout session init response.
     * Checkout sessions don't use ephemeral keys — customer is associated server-side.
     */
    data class CheckoutSession(
        val customer: CheckoutSessionResponse.Customer,
        val offerSave: CheckoutSessionResponse.SavedPaymentMethodsOfferSave?,
    ) : CustomerInfo
}

internal fun CustomerInfo.toCustomerInfo(): CustomerRepository.CustomerInfo? = when (this) {
    is CustomerInfo.CustomerSession -> CustomerRepository.CustomerInfo(
        id = id,
        ephemeralKeySecret = ephemeralKeySecret,
        customerSessionClientSecret = customerSessionClientSecret,
    )
    is CustomerInfo.Legacy -> CustomerRepository.CustomerInfo(
        id = id,
        ephemeralKeySecret = ephemeralKeySecret,
        customerSessionClientSecret = null,
    )
    // Checkout sessions don't use ephemeral keys for customer API calls.
    is CustomerInfo.CheckoutSession -> null
}
