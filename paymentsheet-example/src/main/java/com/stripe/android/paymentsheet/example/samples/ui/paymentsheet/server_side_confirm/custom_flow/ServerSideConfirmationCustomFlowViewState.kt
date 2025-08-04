package com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.server_side_confirm.custom_flow

import com.stripe.android.elements.payment.FlowController
import com.stripe.android.elements.payment.FlowController.PaymentOptionDisplayData
import com.stripe.android.elements.payment.GooglePayConfiguration
import com.stripe.android.paymentsheet.example.samples.model.CartState

data class ServerSideConfirmationCustomFlowViewState(
    val isProcessing: Boolean = false,
    val isError: Boolean = false,
    val confirmedCartState: CartState = CartState.default,
    val dirtyCartState: CartState? = null,
    val status: String? = null,
    val paymentOption: PaymentOptionDisplayData? = null,
    val didComplete: Boolean = false,
) {

    val cartState: CartState
        get() = dirtyCartState ?: confirmedCartState

    val paymentSheetConfig: FlowController.Configuration
        get() = FlowController.Configuration.Builder(merchantDisplayName = "Example, Inc.")
            .customer(cartState.makeCustomerConfig())
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

    val isPaymentMethodButtonEnabled: Boolean
        get() = dirtyCartState == null && !isProcessing

    val isBuyButtonEnabled: Boolean
        get() = dirtyCartState == null && paymentOption != null
}
