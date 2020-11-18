package com.stripe.android.paymentsheet.model

internal sealed class PaymentOptionViewState {
    object Ready : PaymentOptionViewState()

    object Confirming : PaymentOptionViewState()

    object Completed : PaymentOptionViewState()
}
