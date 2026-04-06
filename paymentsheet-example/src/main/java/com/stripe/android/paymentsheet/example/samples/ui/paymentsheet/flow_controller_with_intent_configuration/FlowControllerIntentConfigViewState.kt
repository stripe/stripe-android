package com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.flow_controller_with_intent_configuration

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.samples.model.CartState
import com.stripe.android.paymentsheet.model.PaymentOption

internal data class FlowControllerIntentConfigViewState(
    val isProcessing: Boolean = false,
    val isError: Boolean = false,
    val cartState: CartState = CartState.default,
    val status: String? = null,
    val paymentOption: PaymentOption? = null,
    val didComplete: Boolean = false,
) {
    val paymentSheetConfig: PaymentSheet.Configuration
        get() = PaymentSheet.Configuration.Builder(merchantDisplayName = "Example, Inc.")
            .googlePay(
                PaymentSheet.GooglePayConfiguration(
                    environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                    countryCode = "US",
                )
            )
            .allowsDelayedPaymentMethods(true)
            .build()

    val isPaymentMethodButtonEnabled: Boolean
        get() = !isProcessing

    val isBuyButtonEnabled: Boolean
        get() = paymentOption != null && !isProcessing
}
