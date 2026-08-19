package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.BuildConfig
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
        expressCheckoutElementConfiguration.googlePayConfiguration.asPaymentSheet(
            merchantCountry = merchantCountry,
            liveMode = checkoutSessionResponse.liveMode,
            isDebugBuild = BuildConfig.DEBUG,
        )
    }

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutCollectedDetails.toBillingDetails(
    checkoutSessionResponse: CheckoutSessionResponse,
): PaymentSheet.BillingDetails = PaymentSheet.BillingDetails(
    address = billingAddress?.asPaymentSheet(),
    email = checkoutSessionResponse.customerEmail,
    name = billingName,
)

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutCollectedDetails.toShippingDetails(): AddressDetails = AddressDetails(
    name = shippingName,
    address = shippingAddress?.asPaymentSheet(),
)
