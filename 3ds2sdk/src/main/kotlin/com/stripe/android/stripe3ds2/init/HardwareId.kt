package com.stripe.android.stripe3ds2.init

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class HardwareId internal constructor(
    val value: String
) : Parcelable {
    val isPresent: Boolean
        get() = value.isNotEmpty()
}
