package com.stripe.android.ui.core

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface PaymentSheetMode : Parcelable {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Payment(
        val amount: Long,
        val currency: String,
    ) : PaymentSheetMode

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Setup(
        val currency: String?,
    ) : PaymentSheetMode
}
