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
    buttonType = buttonType,
    additionalEnabledNetworks = additionalEnabledNetworks,
)
