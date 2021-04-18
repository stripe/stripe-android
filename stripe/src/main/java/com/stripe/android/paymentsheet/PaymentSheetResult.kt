package com.stripe.android.paymentsheet

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * The result of a payment sheet operation.
 */
sealed class PaymentSheetResult : Parcelable {

    @Parcelize
    object Completed : PaymentSheetResult()

    @Parcelize
    object Canceled : PaymentSheetResult()

    @Parcelize
    data class Failed(
        val error: Throwable
    ) : PaymentSheetResult()
}
