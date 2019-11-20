package com.stripe.android.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class DateOfBirth(
    val day: Int,
    val month: Int,
    val year: Int
) : Parcelable
