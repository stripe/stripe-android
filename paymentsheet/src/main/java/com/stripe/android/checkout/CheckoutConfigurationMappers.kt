package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutController.Configuration.State.toBillingDetailsCollectionConfiguration(
    checkoutSessionResponse: CheckoutSessionResponse,
): PaymentSheet.BillingDetailsCollectionConfiguration =
    paymentElementConfiguration.billingDetailsCollectionConfiguration
        .reconcile(checkoutSessionResponse.requiresBillingAddress)
        .asPaymentSheet()

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutController.Configuration.State.resolveMerchantDisplayName(
    checkoutSessionResponse: CheckoutSessionResponse,
    appName: String,
): String = merchantDisplayName ?: checkoutSessionResponse.businessName ?: appName

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutController.Configuration.State.toGooglePayConfiguration(
    checkoutSessionResponse: CheckoutSessionResponse,
): PaymentSheet.GooglePayConfiguration? =
    checkoutSessionResponse.merchantCountry?.let { merchantCountry ->
        googlePayConfiguration?.asPaymentSheet(merchantCountry)
    }

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutCollectedDetails.toBillingDetails(
    checkoutSessionResponse: CheckoutSessionResponse,
): PaymentSheet.BillingDetails = PaymentSheet.BillingDetails(
    address = billingAddress?.asPaymentSheet(),
    // The merchant-provided default email is set on the session during configure, so it arrives here
    // as the session's customer email.
    email = checkoutSessionResponse.customerEmail,
    name = billingName,
    phone = billingPhoneNumber,
)

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutCollectedDetails.toShippingDetails(): AddressDetails = AddressDetails(
    name = shippingName,
    address = shippingAddress?.asPaymentSheet(),
    phoneNumber = shippingPhoneNumber,
)
