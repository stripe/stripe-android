package com.stripe.android.paymentsheet.state

import android.os.Parcelable
import com.stripe.android.core.strings.ResolvableString
import kotlinx.parcelize.Parcelize

internal sealed class SavedPaymentMethodSelectionState : Parcelable {
    @Parcelize
    data object Idle : SavedPaymentMethodSelectionState()

    @Parcelize
    data class Pending(val paymentMethodId: String) : SavedPaymentMethodSelectionState()

    @Parcelize
    data class Failed(val error: ResolvableString) : SavedPaymentMethodSelectionState()
}
