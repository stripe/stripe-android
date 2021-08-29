package com.stripe.android.paymentsheet.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

internal sealed class SavedSelection : Parcelable {
    @Parcelize
    object GooglePay : SavedSelection()

    @Parcelize
    data class PaymentMethod(
        val id: String
    ) : SavedSelection()

    @Parcelize
    object None : SavedSelection()
}
