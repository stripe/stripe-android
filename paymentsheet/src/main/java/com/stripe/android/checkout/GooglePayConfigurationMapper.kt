package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet

@OptIn(CheckoutSessionPreview::class)
internal fun GooglePayConfiguration.State.asPaymentSheet(): PaymentSheet.GooglePayConfiguration =
    PaymentSheet.GooglePayConfiguration(
        environment = environment.asPaymentSheet(),
        countryCode = countryCode,
        label = label,
        buttonType = buttonType.asPaymentSheet(),
        additionalEnabledNetworks = additionalEnabledNetworks,
    )

@OptIn(CheckoutSessionPreview::class)
private fun GooglePayConfiguration.Environment.asPaymentSheet():
    PaymentSheet.GooglePayConfiguration.Environment = when (this) {
    GooglePayConfiguration.Environment.Production ->
        PaymentSheet.GooglePayConfiguration.Environment.Production
    GooglePayConfiguration.Environment.Test ->
        PaymentSheet.GooglePayConfiguration.Environment.Test
}

@OptIn(CheckoutSessionPreview::class)
private fun GooglePayConfiguration.ButtonType.asPaymentSheet():
    PaymentSheet.GooglePayConfiguration.ButtonType = when (this) {
    GooglePayConfiguration.ButtonType.Buy ->
        PaymentSheet.GooglePayConfiguration.ButtonType.Buy
    GooglePayConfiguration.ButtonType.Book ->
        PaymentSheet.GooglePayConfiguration.ButtonType.Book
    GooglePayConfiguration.ButtonType.Checkout ->
        PaymentSheet.GooglePayConfiguration.ButtonType.Checkout
    GooglePayConfiguration.ButtonType.Donate ->
        PaymentSheet.GooglePayConfiguration.ButtonType.Donate
    GooglePayConfiguration.ButtonType.Order ->
        PaymentSheet.GooglePayConfiguration.ButtonType.Order
    GooglePayConfiguration.ButtonType.Pay ->
        PaymentSheet.GooglePayConfiguration.ButtonType.Pay
    GooglePayConfiguration.ButtonType.Subscribe ->
        PaymentSheet.GooglePayConfiguration.ButtonType.Subscribe
    GooglePayConfiguration.ButtonType.Plain ->
        PaymentSheet.GooglePayConfiguration.ButtonType.Plain
}
