package com.stripe.android.payments.samsungpay

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed interface SamsungPayResult : Parcelable {
    @Parcelize
    data object Success : SamsungPayResult

    @Parcelize
    data object Cancel : SamsungPayResult

    @Parcelize
    data class Failure(val error: Throwable) : SamsungPayResult
}