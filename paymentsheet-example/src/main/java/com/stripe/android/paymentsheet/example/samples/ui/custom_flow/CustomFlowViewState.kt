package com.stripe.android.paymentsheet.example.samples.ui.custom_flow

import android.graphics.drawable.Drawable
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.samples.model.CartState

data class CustomFlowViewState(
    val isProcessing: Boolean = false,
    val cartState: CartState = CartState.default,
    val publishableKey: String? = null,
    val clientSecret: String? = null,
    val customerConfig: PaymentSheet.CustomerConfiguration? = null,
    val status: String? = null,
    val paymentOptionLabel: String? = null,
    val paymentOptionIcon: Drawable? = null,
    val didComplete: Boolean = false,
) {

    val paymentSheetConfig: PaymentSheet.Configuration
        get() = PaymentSheet.Configuration(
            merchantDisplayName = "Example, Inc.",
            customer = customerConfig,
            googlePay = PaymentSheet.GooglePayConfiguration(
                environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                countryCode = "US",
            ),
            // Set `allowsDelayedPaymentMethods` to true if your business can handle payment
            // methods that complete payment after a delay, like SEPA Debit and Sofort.
            allowsDelayedPaymentMethods = true
        )

    val isPaymentMethodButtonEnabled: Boolean
        get() = !isProcessing && !didComplete

    val isBuyButtonEnabled: Boolean
        get() = !isProcessing && !didComplete && paymentOptionLabel != null
}
