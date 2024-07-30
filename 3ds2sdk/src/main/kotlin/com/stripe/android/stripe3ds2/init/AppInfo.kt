package com.stripe.android.stripe3ds2.init

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class AppInfo(
    val sdkAppId: String,
    val version: Int
) : Parcelable
