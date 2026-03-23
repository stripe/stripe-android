package com.stripe.android.paymentsheet.state

import com.stripe.android.common.model.CommonConfiguration
import com.stripe.android.common.model.PaymentMethodRemovePermission
import com.stripe.android.core.exception.StripeException
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.lpmfoundations.paymentmethod.toSaveConsentBehavior
import com.stripe.android.model.ElementsSession
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import javax.inject.Inject

/**
 * Creates a [CustomerMetadata] from the given [PaymentElementLoader.InitializationMode],
 * [CommonConfiguration], and [ElementsSession].
 *
 * For [PaymentElementLoader.InitializationMode.CheckoutSession], builds a
 * [CustomerMetadata.CheckoutSession] from the checkout session response.
 * For other modes, dispatches on [CommonConfiguration.customer]'s access type to build
 * either a [CustomerMetadata.CustomerSession] or [CustomerMetadata.LegacyEphemeralKey].
 */
internal class CreateCustomerMetadata @Inject constructor(
    private val errorReporter: ErrorReporter,
) {

    operator fun invoke(
        initializationMode: PaymentElementLoader.InitializationMode,
        configuration: CommonConfiguration,
        elementsSession: ElementsSession,
    ): CustomerMetadata? {
        if (initializationMode is PaymentElementLoader.InitializationMode.CheckoutSession) {
            val customer = initializationMode.checkoutSessionResponse.customer ?: return null
            return CustomerMetadata.CheckoutSession(
                sessionId = initializationMode.checkoutSessionResponse.id,
                customerId = customer.id,
                removePaymentMethod = if (customer.canDetachPaymentMethod) {
                    PaymentMethodRemovePermission.Full
                } else {
                    PaymentMethodRemovePermission.None
                },
                saveConsent = initializationMode.checkoutSessionResponse.savedPaymentMethodsOfferSave
                    ?.toSaveConsentBehavior()
                    ?: PaymentMethodSaveConsentBehavior.Disabled(overrideAllowRedisplay = null),
            )
        }

        return when (val accessType = configuration.customer?.accessType) {
            is PaymentSheet.CustomerAccessType.CustomerSession -> {
                val customer = elementsSession.customer ?: run {
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

                    return null
                }
                CustomerMetadata.createForPaymentSheetCustomerSession(
                    configuration = configuration,
                    customer = customer,
                    id = customer.session.customerId,
                    ephemeralKeySecret = customer.session.apiKey,
                    customerSessionClientSecret = accessType.customerSessionClientSecret,
                    isPaymentMethodSetAsDefaultEnabled = getDefaultPaymentMethodsEnabled(elementsSession),
                )
            }
            is PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey -> {
                CustomerMetadata.createForPaymentSheetLegacyEphemeralKey(
                    configuration = configuration,
                    id = configuration.customer.id,
                    ephemeralKeySecret = accessType.ephemeralKeySecret,
                    isPaymentMethodSetAsDefaultEnabled = getDefaultPaymentMethodsEnabled(elementsSession),
                )
            }
            null -> null
        }
    }

    private fun getDefaultPaymentMethodsEnabled(elementsSession: ElementsSession): Boolean {
        val mobilePaymentElement = elementsSession.customer?.session?.components?.mobilePaymentElement
            as? ElementsSession.Customer.Components.MobilePaymentElement.Enabled
        return mobilePaymentElement?.isPaymentMethodSetAsDefaultEnabled
            ?: false
    }
}
