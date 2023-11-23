package com.stripe.android.uicore.forms

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
data class FormFieldEntry(
    val value: String?,
    val isComplete: Boolean = false
) : Parcelable
