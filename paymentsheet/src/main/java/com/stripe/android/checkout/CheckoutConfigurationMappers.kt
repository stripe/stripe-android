package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.BuildConfig
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutSessionResponse.toBillingDetailsCollectionConfiguration():
    PaymentSheet.BillingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
    address = if (requiresBillingAddress) {
        PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
    } else {
        PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic
    },
    attachDefaultsToPaymentMethod = true,
)

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutController.Configuration.State.resolveMerchantDisplayName(
    checkoutSessionResponse: CheckoutSessionResponse,
    appName: String,
): String = merchantDisplayName ?: checkoutSessionResponse.businessName ?: appName

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutController.Configuration.State.toExpressCheckoutElementGooglePayConfiguration(
    checkoutSessionResponse: CheckoutSessionResponse,
): PaymentSheet.GooglePayConfiguration? =
    expressCheckoutElementConfiguration?.let { configuration ->
        checkoutSessionResponse.merchantCountry?.let { merchantCountry ->
            configuration.googlePayConfiguration.asPaymentSheet(
                merchantCountry = merchantCountry,
                liveMode = checkoutSessionResponse.liveMode,
                isDebugBuild = BuildConfig.DEBUG,
            )
        }
    }

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutController.Configuration.State.toPaymentElementGooglePayConfiguration(
    checkoutSessionResponse: CheckoutSessionResponse,
): PaymentSheet.GooglePayConfiguration? =
    checkoutSessionResponse.merchantCountry?.let { merchantCountry ->
        paymentElementConfiguration.googlePayConfiguration.asPaymentSheet(
            merchantCountry = merchantCountry,
            liveMode = checkoutSessionResponse.liveMode,
            isDebugBuild = BuildConfig.DEBUG,
        )
    }

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutController.Configuration.State.toBillingDetails(
    checkoutSessionResponse: CheckoutSessionResponse,
    collectedEmail: String?,
): PaymentSheet.BillingDetails = PaymentSheet.BillingDetails(
    address = defaults.billingDetails?.address?.asPaymentSheet(),
    email = collectedEmail ?: checkoutSessionResponse.customerEmail,
    name = defaults.billingDetails?.name,
)

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutCollectedDetails.toShippingDetails(): AddressDetails = AddressDetails(
    name = shippingName,
    address = shippingAddress?.asPaymentSheet(),
)
