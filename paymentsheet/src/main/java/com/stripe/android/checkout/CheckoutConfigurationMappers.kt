@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.checkout

import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

internal fun CheckoutController.Configuration.State.toBillingDetailsCollectionConfiguration(
    checkoutSessionResponse: CheckoutSessionResponse,
): PaymentSheet.BillingDetailsCollectionConfiguration =
    paymentElementConfiguration.billingDetailsCollectionConfiguration
        .reconcile(checkoutSessionResponse.requiresBillingAddress)
        .asPaymentSheet()

internal fun CheckoutController.Configuration.State.toGooglePayConfiguration(
    checkoutSessionResponse: CheckoutSessionResponse,
): PaymentSheet.GooglePayConfiguration? =
    checkoutSessionResponse.merchantCountry?.let { merchantCountry ->
        expressCheckoutElementConfiguration.googlePayConfiguration?.asPaymentSheet(merchantCountry)
    }

internal fun CheckoutController.Configuration.State.resolveExpressCheckoutElementConfiguration(
    checkoutSessionResponse: CheckoutSessionResponse,
): CheckoutController.Configuration.State {
    val expressConfiguration = expressCheckoutElementConfiguration
    val googlePayConfiguration = expressConfiguration.googlePayConfiguration ?: return this
    val shippingAddressParameters = if (expressConfiguration.shippingAddressRequired) {
        GooglePayJsonFactory.ShippingAddressParameters(
            isRequired = true,
            allowedCountryCodes = checkoutSessionResponse.allowedShippingCountries.orEmpty().toSet(),
        )
    } else {
        null
    }

    return copy(
        expressCheckoutElementConfiguration = expressConfiguration.copy(
            googlePayConfiguration = googlePayConfiguration.copy(
                shippingAddressParameters = shippingAddressParameters,
            ),
        ),
    )
}

internal fun CheckoutCollectedDetails.toBillingDetails(
    checkoutSessionResponse: CheckoutSessionResponse,
): PaymentSheet.BillingDetails = PaymentSheet.BillingDetails(
    address = billingAddress?.asPaymentSheet(),
    email = checkoutSessionResponse.customerEmail,
    name = billingName,
    phone = billingPhoneNumber,
)

internal fun CheckoutCollectedDetails.toShippingDetails(): AddressDetails = AddressDetails(
    name = shippingName,
    address = shippingAddress?.asPaymentSheet(),
    phoneNumber = shippingPhoneNumber,
)
