package com.stripe.android.payments.samsungpay

import android.os.Parcelable
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@Parcelize
@Poko
class Config(
    internal val amount: Long
): Parcelable