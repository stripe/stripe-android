package com.stripe.form

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Immutable
@Parcelize
data class ValueChange<T>(
    val key: Key<T>,
    val value: @RawValue T,
    val isComplete: Boolean,
) : Parcelable
