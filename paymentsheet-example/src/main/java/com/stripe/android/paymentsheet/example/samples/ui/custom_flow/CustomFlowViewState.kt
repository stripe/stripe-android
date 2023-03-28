package com.stripe.android.paymentsheet.example.samples.ui.custom_flow

import android.graphics.drawable.Drawable
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.samples.model.CartState

data class CustomFlowViewState(
    val isProcessing: Boolean = false,
    val cartState: CartState = CartState.default,
    val paymentInfo: PaymentInfo? = null,
    val status: String? = null,
    val paymentOptionLabel: String? = null,
    val paymentOptionIcon: Drawable? = null,
    val didComplete: Boolean = false,
) {

    val isPaymentMethodButtonEnabled: Boolean
        get() = !isProcessing && !didComplete

    val isBuyButtonEnabled: Boolean
        get() = !isProcessing && !didComplete && paymentOptionLabel != null

    data class PaymentInfo(
        val publishableKey: String,
        val clientSecret: String,
        val customerConfiguration: PaymentSheet.CustomerConfiguration?,
    ) {

        val paymentSheetConfig: PaymentSheet.Configuration
            get() = PaymentSheet.Configuration(
                merchantDisplayName = "Example, Inc.",
                customer = customerConfiguration,
                googlePay = PaymentSheet.GooglePayConfiguration(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                    countryCode = "US",
                ),
                // Set `allowsDelayedPaymentMethods` to true if your business can handle payment
                // methods that complete payment after a delay, like SEPA Debit and Sofort.
                allowsDelayedPaymentMethods = true
            )
    }
}
