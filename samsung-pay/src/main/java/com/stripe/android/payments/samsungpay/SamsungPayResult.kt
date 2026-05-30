package com.stripe.android.payments.samsungpay

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed interface SamsungPayResult : Parcelable {
    @Parcelize
    data object Completed : SamsungPayResult

    @Parcelize
    data object Canceled : SamsungPayResult

    @Parcelize
    data class Failed(val error: Throwable) : SamsungPayResult
}
