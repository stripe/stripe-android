package com.stripe.android.paymentsheet.model

internal sealed class PaymentOptionViewState {
    object Ready : PaymentOptionViewState()

    object Processing : PaymentOptionViewState()

    data class Completed(
        val paymentSelection: PaymentSelection
    ) : PaymentOptionViewState()
}
