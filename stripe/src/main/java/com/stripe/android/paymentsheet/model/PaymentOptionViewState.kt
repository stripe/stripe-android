package com.stripe.android.paymentsheet.model

internal sealed class PaymentOptionViewState {
    object Ready : PaymentOptionViewState()

    data class Completed(
        val paymentSelection: PaymentSelection
    ) : PaymentOptionViewState()
}
