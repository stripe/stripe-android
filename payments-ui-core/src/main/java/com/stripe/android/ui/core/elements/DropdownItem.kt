package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class DropdownItem(
    val apiValue: String?,
    val displayText: String,
)
