package com.stripe.android.paymentsheet.example.samples.ui.embedded_payment_element

import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.samples.model.CartState

internal data class EmbeddedPaymentElementExampleViewState(
    val isProcessing: Boolean = false,
    val isError: Boolean = false,
    val cartState: CartState = CartState.default,
    val status: String? = null,
    val didComplete: Boolean = false,
) {
    val embeddedConfig: EmbeddedPaymentElement.Configuration
        get() = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
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
}
