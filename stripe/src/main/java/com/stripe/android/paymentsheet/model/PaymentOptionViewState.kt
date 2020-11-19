package com.stripe.android.paymentsheet.model

internal sealed class PaymentOptionViewState {
    object Ready : PaymentOptionViewState()

    object Processing : PaymentOptionViewState()

    object Completed : PaymentOptionViewState()
}
