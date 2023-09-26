package com.stripe.android.uicore.elements

import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface SingleChoiceDropdownItem {
    val label: String

    @get:DrawableRes
    val icon: Int?
}
