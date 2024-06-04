package com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.complete_flow

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.samples.model.CartState

data class CompleteFlowViewState(
    val isProcessing: Boolean = false,
    val paymentInfo: PaymentInfo? = null,
    val cartState: CartState = CartState.default,
    val status: String? = null,
    val didComplete: Boolean = false,
) {

    data class PaymentInfo(
        val clientSecret: String,
        val customerConfiguration: PaymentSheet.CustomerConfiguration?,
        val shouldPresent: Boolean,
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
