package com.stripe.android.paymentsheet.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

internal sealed interface SepaMandateResult : Parcelable {
    @Parcelize
    object Canceled : SepaMandateResult

    @Parcelize
    object Acknowledged : SepaMandateResult
}
