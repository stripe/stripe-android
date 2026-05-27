package com.stripe.android.testing

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class QuarantinedTestMatch(
    val className: String,
    val testCaseName: String,
) : Parcelable
