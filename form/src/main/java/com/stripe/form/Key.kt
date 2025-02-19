package com.stripe.form

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.util.UUID

@Parcelize
data class Key<T>(val key: @RawValue Any): Parcelable

fun <T> key(name: String = UUID.randomUUID().toString()) = Key<T>(name)