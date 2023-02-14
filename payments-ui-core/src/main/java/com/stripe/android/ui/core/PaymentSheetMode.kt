package com.stripe.android.ui.core

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface PaymentSheetMode : Parcelable {

    @Parcelize
    data class Payment(val amount: Long, val currency: String) : PaymentSheetMode

    @Parcelize
    data class Setup(val currency: String?) : PaymentSheetMode
}
