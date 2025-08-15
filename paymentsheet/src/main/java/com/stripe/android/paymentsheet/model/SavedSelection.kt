package com.stripe.android.paymentsheet.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

internal sealed class SavedSelection : Parcelable {
    @Parcelize
    data object GooglePay : SavedSelection()

    @Parcelize
    data object Link : SavedSelection()

    @Parcelize
    data class PaymentMethod(
        val id: String,
        val isLinkOrigin: Boolean = false,
    ) : SavedSelection()

    @Parcelize
    data object None : SavedSelection()
}

internal fun PaymentSelection.toSavedSelection(): SavedSelection? {
    return when (this) {
        is PaymentSelection.GooglePay -> SavedSelection.GooglePay
        is PaymentSelection.Link -> SavedSelection.Link
        is PaymentSelection.Saved -> SavedSelection.PaymentMethod(
            id = paymentMethod.id.orEmpty(),
            isLinkOrigin = paymentMethod.isLinkPaymentMethod || paymentMethod.isLinkPassthroughMode,
        )
        else -> null
    }
}
