package com.stripe.android.ui.core.forms

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class FormFieldEntry(
    val value: String?,
    val isComplete: Boolean = false
)
