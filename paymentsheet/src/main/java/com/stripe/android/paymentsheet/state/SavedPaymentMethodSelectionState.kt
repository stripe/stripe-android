package com.stripe.android.paymentsheet.state

internal sealed class SavedPaymentMethodSelectionState {
    data object Idle : SavedPaymentMethodSelectionState()

    data object Pending : SavedPaymentMethodSelectionState()
}
