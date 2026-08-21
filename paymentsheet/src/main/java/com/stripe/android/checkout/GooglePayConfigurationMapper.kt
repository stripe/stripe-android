package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.elements.ExpressCheckoutElement.Configuration.GooglePayConfiguration as EceGooglePayConfiguration
import com.stripe.android.elements.PaymentElement.Configuration.GooglePayConfiguration as PeGooglePayConfiguration

@OptIn(CheckoutSessionPreview::class)
internal fun EceGooglePayConfiguration.State.asPaymentSheet(
    merchantCountry: String,
    liveMode: Boolean,
    isDebugBuild: Boolean,
): PaymentSheet.GooglePayConfiguration = PaymentSheet.GooglePayConfiguration(
    environment = if (liveMode && !isDebugBuild) {
        PaymentSheet.GooglePayConfiguration.Environment.Production
    } else {
        PaymentSheet.GooglePayConfiguration.Environment.Test
    },
    countryCode = merchantCountry,
    label = label,
    buttonType = buttonType.asPaymentSheet(),
    additionalEnabledNetworks = additionalEnabledNetworks,
)

@OptIn(CheckoutSessionPreview::class)
private fun EceGooglePayConfiguration.ButtonType.asPaymentSheet():
    PaymentSheet.GooglePayConfiguration.ButtonType = when (this) {
    EceGooglePayConfiguration.ButtonType.Buy ->
        PaymentSheet.GooglePayConfiguration.ButtonType.Buy
    EceGooglePayConfiguration.ButtonType.Book ->
        PaymentSheet.GooglePayConfiguration.ButtonType.Book
    EceGooglePayConfiguration.ButtonType.Checkout ->
        PaymentSheet.GooglePayConfiguration.ButtonType.Checkout
    EceGooglePayConfiguration.ButtonType.Donate ->
        PaymentSheet.GooglePayConfiguration.ButtonType.Donate
    EceGooglePayConfiguration.ButtonType.Order ->
        PaymentSheet.GooglePayConfiguration.ButtonType.Order
    EceGooglePayConfiguration.ButtonType.Pay ->
        PaymentSheet.GooglePayConfiguration.ButtonType.Pay
    EceGooglePayConfiguration.ButtonType.Subscribe ->
        PaymentSheet.GooglePayConfiguration.ButtonType.Subscribe
    EceGooglePayConfiguration.ButtonType.Plain ->
        PaymentSheet.GooglePayConfiguration.ButtonType.Plain
}

@OptIn(CheckoutSessionPreview::class)
internal fun PeGooglePayConfiguration.State.asPaymentSheet(
    merchantCountry: String,
    liveMode: Boolean,
    isDebugBuild: Boolean,
): PaymentSheet.GooglePayConfiguration = PaymentSheet.GooglePayConfiguration(
    environment = if (liveMode && !isDebugBuild) {
        PaymentSheet.GooglePayConfiguration.Environment.Production
    } else {
        PaymentSheet.GooglePayConfiguration.Environment.Test
    },
    countryCode = merchantCountry,
    label = label,
    buttonType = buttonType.asPaymentSheet(),
    additionalEnabledNetworks = additionalEnabledNetworks,
)

@OptIn(CheckoutSessionPreview::class)
private fun PeGooglePayConfiguration.ButtonType.asPaymentSheet():
    PaymentSheet.GooglePayConfiguration.ButtonType = when (this) {
    PeGooglePayConfiguration.ButtonType.Buy -> PaymentSheet.GooglePayConfiguration.ButtonType.Buy
    PeGooglePayConfiguration.ButtonType.Book -> PaymentSheet.GooglePayConfiguration.ButtonType.Book
    PeGooglePayConfiguration.ButtonType.Checkout -> PaymentSheet.GooglePayConfiguration.ButtonType.Checkout
    PeGooglePayConfiguration.ButtonType.Donate -> PaymentSheet.GooglePayConfiguration.ButtonType.Donate
    PeGooglePayConfiguration.ButtonType.Order -> PaymentSheet.GooglePayConfiguration.ButtonType.Order
    PeGooglePayConfiguration.ButtonType.Pay -> PaymentSheet.GooglePayConfiguration.ButtonType.Pay
    PeGooglePayConfiguration.ButtonType.Subscribe -> PaymentSheet.GooglePayConfiguration.ButtonType.Subscribe
    PeGooglePayConfiguration.ButtonType.Plain -> PaymentSheet.GooglePayConfiguration.ButtonType.Plain
}
