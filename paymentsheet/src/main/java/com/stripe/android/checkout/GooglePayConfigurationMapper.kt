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
    environment = when (environment) {
        CheckoutGooglePayConfiguration.Environment.Production ->
            PaymentSheet.GooglePayConfiguration.Environment.Production
        CheckoutGooglePayConfiguration.Environment.Test ->
            PaymentSheet.GooglePayConfiguration.Environment.Test
        CheckoutGooglePayConfiguration.Environment.Automatic -> if (liveMode && !isDebugBuild) {
            PaymentSheet.GooglePayConfiguration.Environment.Production
        } else {
            PaymentSheet.GooglePayConfiguration.Environment.Test
        }
    },
    countryCode = merchantCountry,
    label = label,
    buttonType = buttonType,
    additionalEnabledNetworks = additionalEnabledNetworks,
)
