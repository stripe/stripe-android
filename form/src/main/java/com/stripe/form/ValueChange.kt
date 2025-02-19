package com.stripe.form

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Immutable
@Parcelize
data class ValueChange<T>(
    val key: Parcelable,
    val value: @RawValue T,
    val isComplete: Boolean,
) : Parcelable
