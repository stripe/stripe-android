package com.stripe.android.paymentelement.confirmation

import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.checkout.CheckoutInstances
import com.stripe.android.checkout.CheckoutSession
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.lpmfoundations.paymentmethod.IntegrationMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

@OptIn(CheckoutSessionPreview::class)
internal data class CheckoutSessionLiveState(
    val amount: Long,
    val currency: String,
    val customerEmail: String?,
)

@OptIn(CheckoutSessionPreview::class)
internal fun PaymentMethodMetadata.currentCheckoutSessionLiveState(): CheckoutSessionLiveState? {
    val checkoutSession = integrationMetadata as? IntegrationMetadata.CheckoutSession ?: return null
    return checkoutSession.currentCheckoutSessionLiveState()
}

@OptIn(CheckoutSessionPreview::class)
internal fun PaymentMethodMetadata.currentCheckoutSessionTaxStatus(): CheckoutSession.Tax.Status? {
    val checkoutSession = integrationMetadata as? IntegrationMetadata.CheckoutSession ?: return null
    return CheckoutInstances[checkoutSession.instancesKey]?.checkoutSession?.value?.tax?.status
}

internal fun PaymentMethodMetadata.googlePayBillingAddressParameters(): GooglePayJsonFactory.BillingAddressParameters {
    val configuration = billingDetailsCollectionConfiguration
    return GooglePayJsonFactory.BillingAddressParameters(
        isRequired = shouldRequireGooglePayBillingAddress(),
        format = when (configuration.address) {
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full -> {
                GooglePayJsonFactory.BillingAddressParameters.Format.Full
            }
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never -> {
                GooglePayJsonFactory.BillingAddressParameters.Format.Min
            }
        },
        isPhoneNumberRequired = configuration.collectsPhone,
    )
}

internal fun PaymentMethodMetadata.googlePayBillingAddressConfig(): GooglePayPaymentMethodLauncher.BillingAddressConfig {
    val configuration = billingDetailsCollectionConfiguration
    return GooglePayPaymentMethodLauncher.BillingAddressConfig(
        isRequired = shouldRequireGooglePayBillingAddress(),
        format = when (configuration.address) {
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full -> {
                GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Full
            }
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic,
            PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never -> {
                GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Min
            }
        },
        isPhoneNumberRequired = configuration.collectsPhone,
    )
}

internal fun PaymentMethodMetadata.requiresGooglePayEmailCollection(): Boolean {
    return when (billingDetailsCollectionConfiguration.email) {
        PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always -> true
        PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never -> false
        PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic -> {
            currentCheckoutSessionLiveState()?.customerEmail == null
        }
    }
}

@OptIn(CheckoutSessionPreview::class)
internal fun IntegrationMetadata.CheckoutSession.currentCheckoutSessionLiveState(): CheckoutSessionLiveState? {
    val response = CheckoutInstances[instancesKey]?.internalState?.checkoutSessionResponse ?: return null
    return CheckoutSessionLiveState(
        amount = response.amount,
        currency = response.currency,
        customerEmail = response.customerEmail,
    )
}

@OptIn(CheckoutSessionPreview::class)
private fun PaymentMethodMetadata.shouldCollectGooglePayBillingAddress(): Boolean {
    val checkoutSession = integrationMetadata as? IntegrationMetadata.CheckoutSession ?: return false
    val response = CheckoutInstances[checkoutSession.instancesKey]
        ?.internalState
        ?.checkoutSessionResponse
        ?: return false

    return billingDetailsCollectionConfiguration.address !=
        PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never &&
        response.taxAddressSource == CheckoutSessionResponse.TaxAddressSource.BILLING
}

private fun PaymentMethodMetadata.shouldRequireGooglePayBillingAddress(): Boolean {
    return billingDetailsCollectionConfiguration.address ==
        PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full ||
        billingDetailsCollectionConfiguration.collectsPhone ||
        shouldCollectGooglePayBillingAddress()
}
