package com.stripe.android.paymentsheet.ui

internal sealed interface PaymentOptionEditState {
    object None : PaymentOptionEditState
    object Removable : PaymentOptionEditState
    object Modifiable : PaymentOptionEditState
}
