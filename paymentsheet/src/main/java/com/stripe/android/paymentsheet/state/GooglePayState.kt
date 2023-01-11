package com.stripe.android.paymentsheet.state

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal sealed class GooglePayState(
    val isReadyForUse: Boolean
) : Parcelable {
    @Parcelize
    object Available : GooglePayState(true)

    @Parcelize
    object NotAvailable : GooglePayState(false)

    @Parcelize
    object Indeterminate : GooglePayState(false)
}
