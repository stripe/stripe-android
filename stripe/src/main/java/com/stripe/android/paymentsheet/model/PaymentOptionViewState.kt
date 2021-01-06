package com.stripe.android.paymentsheet.model

internal sealed class PaymentOptionViewState {
    data class Completed(
        val paymentSelection: PaymentSelection
    ) : PaymentOptionViewState()
}
