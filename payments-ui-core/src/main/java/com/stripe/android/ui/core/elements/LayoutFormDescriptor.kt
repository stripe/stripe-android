package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class LayoutFormDescriptor(
    val layoutSpec: LayoutSpec?,
    val showCheckbox: Boolean,
    val showCheckboxControlledFields: Boolean
)
