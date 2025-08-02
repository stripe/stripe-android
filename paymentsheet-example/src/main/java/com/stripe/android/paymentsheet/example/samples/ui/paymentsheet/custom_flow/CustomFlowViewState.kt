package com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.custom_flow

import com.stripe.android.elements.CustomerConfiguration
import com.stripe.android.elements.payment.FlowController.PaymentOptionDisplayData
import com.stripe.android.elements.payment.GooglePayConfiguration
import com.stripe.android.elements.payment.PaymentSheet
import com.stripe.android.paymentsheet.example.samples.model.CartState

data class CustomFlowViewState(
    val isProcessing: Boolean = false,
    val isError: Boolean = false,
    val cartState: CartState = CartState.default,
    val paymentInfo: PaymentInfo? = null,
    val status: String? = null,
    val paymentOption: PaymentOptionDisplayData? = null,
    val didComplete: Boolean = false,
) {

    val isPaymentMethodButtonEnabled: Boolean
        get() = !isProcessing && !didComplete

    val isBuyButtonEnabled: Boolean
        get() = !isProcessing && !didComplete && paymentOption != null

    data class PaymentInfo(
        val clientSecret: String,
        val customerConfiguration: CustomerConfiguration?,
    ) {

        val paymentSheetConfig: PaymentSheet.Configuration
            get() = PaymentSheet.Configuration.Builder(merchantDisplayName = "Example, Inc.")
                .customer(customerConfiguration)
                .googlePay(
                    GooglePayConfiguration(
                        environment = GooglePayConfiguration.Environment.Test,
                        countryCode = "US",
                    )
                )
                // Set `allowsDelayedPaymentMethods` to true if your business can handle payment
                // methods that complete payment after a delay, like SEPA Debit and Sofort.
                .allowsDelayedPaymentMethods(true)
                .build()
    }
}
