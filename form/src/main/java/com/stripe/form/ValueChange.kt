package com.stripe.form

import android.os.Parcelable
import androidx.compose.runtime.Immutable

@Immutable
data class ValueChange<T>(
    val key: Parcelable,
    val value: T,
    val isComplete: Boolean,
)