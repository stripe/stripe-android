package com.stripe.android.uicore.elements

import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import com.stripe.android.core.strings.ResolvableString

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface SingleChoiceDropdownItem {
    val label: ResolvableString

    @get:DrawableRes
    val icon: Int?

    val enabled: Boolean
}
