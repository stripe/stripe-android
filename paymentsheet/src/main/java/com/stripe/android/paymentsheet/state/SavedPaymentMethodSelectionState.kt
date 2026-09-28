package com.stripe.android.paymentsheet.state

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

internal sealed class SavedPaymentMethodSelectionState : Parcelable {
    @Parcelize
    data object Idle : SavedPaymentMethodSelectionState()

    @Parcelize
    data class Pending(val paymentMethodId: String) : SavedPaymentMethodSelectionState()
}
