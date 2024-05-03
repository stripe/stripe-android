package com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.server_side_confirm.custom_flow

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.samples.model.CartState
import com.stripe.android.paymentsheet.model.PaymentOption

data class ServerSideConfirmationCustomFlowViewState(
    val isProcessing: Boolean = false,
    val isError: Boolean = false,
    val confirmedCartState: CartState = CartState.default,
    val dirtyCartState: CartState? = null,
    val status: String? = null,
    val paymentOption: PaymentOption? = null,
    val didComplete: Boolean = false,
) {

    val cartState: CartState
        get() = dirtyCartState ?: confirmedCartState

    val paymentSheetConfig: PaymentSheet.Configuration
        get() = PaymentSheet.Configuration(
            merchantDisplayName = "Example, Inc.",
            customer = cartState.makeCustomerConfig(),
            googlePay = PaymentSheet.GooglePayConfiguration(
                environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
                countryCode = "US",
            ),
            // Set `allowsDelayedPaymentMethods` to true if your business can handle payment
            // methods that complete payment after a delay, like SEPA Debit and Sofort.
            allowsDelayedPaymentMethods = true,
        )

    val isPaymentMethodButtonEnabled: Boolean
        get() = dirtyCartState == null && !isProcessing

    val isBuyButtonEnabled: Boolean
        get() = dirtyCartState == null && paymentOption != null
}
