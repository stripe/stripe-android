package com.stripe.android.checkout

import com.stripe.android.elements.CheckoutGooglePayConfiguration
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet

@OptIn(CheckoutSessionPreview::class)
internal fun CheckoutGooglePayConfiguration.asPaymentSheet(
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
private fun CheckoutGooglePayConfiguration.ButtonType.asPaymentSheet():
    PaymentSheet.GooglePayConfiguration.ButtonType = when (this) {
    CheckoutGooglePayConfiguration.ButtonType.Buy -> PaymentSheet.GooglePayConfiguration.ButtonType.Buy
    CheckoutGooglePayConfiguration.ButtonType.Book -> PaymentSheet.GooglePayConfiguration.ButtonType.Book
    CheckoutGooglePayConfiguration.ButtonType.Checkout -> PaymentSheet.GooglePayConfiguration.ButtonType.Checkout
    CheckoutGooglePayConfiguration.ButtonType.Donate -> PaymentSheet.GooglePayConfiguration.ButtonType.Donate
    CheckoutGooglePayConfiguration.ButtonType.Order -> PaymentSheet.GooglePayConfiguration.ButtonType.Order
    CheckoutGooglePayConfiguration.ButtonType.Pay -> PaymentSheet.GooglePayConfiguration.ButtonType.Pay
    CheckoutGooglePayConfiguration.ButtonType.Subscribe -> PaymentSheet.GooglePayConfiguration.ButtonType.Subscribe
    CheckoutGooglePayConfiguration.ButtonType.Plain -> PaymentSheet.GooglePayConfiguration.ButtonType.Plain
}
