package com.stripe.form

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@JvmInline
@Parcelize
value class ParcelableKey(val key: String) : Parcelable

fun parcelableKey(key: String) = ParcelableKey(key)
