package com.stripe.android.paymentsheet.state

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.core.exception.StripeException
import com.stripe.android.model.ElementsSession
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import javax.inject.Inject

internal data class SessionResult(
    val elementsSession: ElementsSession,
    val customerInfo: CustomerInfo?,
)

/**
 * Extracts session data from the [CheckoutSessionResponse] already loaded during [Checkout.configure].
 *
 * The checkout session init response contains the elements session and customer data.
 */
internal class CheckoutSessionLoader @Inject constructor() {
    operator fun invoke(
        initializationMode: PaymentElementLoader.InitializationMode.CheckoutSession,
    ): SessionResult {
        val checkoutSession = initializationMode.checkoutSessionResponse

        val elementsSession = checkoutSession.elementsSession
            ?: throw IllegalStateException("CheckoutSession init response missing elements_session")

        val customerInfo = checkoutSession.customer?.let { customer ->
            CustomerInfo.CheckoutSession(
                sessionId = checkoutSession.id,
                customer = customer,
                offerSave = checkoutSession.savedPaymentMethodsOfferSave,
            )
        }

        return SessionResult(
            elementsSession = elementsSession,
            customerInfo = customerInfo,
        )
    }
}

/**
 * Loads session data via [ElementsSessionRepository] for standard (non-checkout) flows.
 *
 * Customer info is derived from the merchant's configuration and the elements session response.
 */
internal class ElementsSessionLoader @Inject constructor(
    private val elementsSessionRepository: ElementsSessionRepository,
    private val errorReporter: ErrorReporter,
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

        val customerInfo = createCustomerInfo(
            configuration = configuration,
            elementsSession = elementsSession,
        )

        return SessionResult(
            elementsSession = elementsSession,
            customerInfo = customerInfo,
        )
    }

    private fun createCustomerInfo(
        configuration: CommonConfiguration,
        elementsSession: ElementsSession,
    ): CustomerInfo? {
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
        val sessionId: String,
        val customer: CheckoutSessionResponse.Customer,
        val offerSave: CheckoutSessionResponse.SavedPaymentMethodsOfferSave?,
    ) : CustomerInfo
}

internal fun CustomerInfo.customerIdOrNull(): String? = when (this) {
    is CustomerInfo.CustomerSession -> id
    is CustomerInfo.Legacy -> id
    is CustomerInfo.CheckoutSession -> null
}

internal fun CustomerInfo.ephemeralKeySecretOrNull(): String? = when (this) {
    is CustomerInfo.CustomerSession -> ephemeralKeySecret
    is CustomerInfo.Legacy -> ephemeralKeySecret
    is CustomerInfo.CheckoutSession -> null
}

internal fun CustomerInfo.customerEmailOrNull(): String? = when (this) {
    is CustomerInfo.CheckoutSession -> customer.email
    is CustomerInfo.CustomerSession -> null
    is CustomerInfo.Legacy -> null
}
