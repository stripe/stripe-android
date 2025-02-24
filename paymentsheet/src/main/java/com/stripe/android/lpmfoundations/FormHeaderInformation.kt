package com.stripe.android.lpmfoundations

import androidx.annotation.DrawableRes
import com.stripe.android.core.strings.ResolvableString

internal data class FormHeaderInformation(
    val displayName: ResolvableString,
    val shouldShowIcon: Boolean,
    @DrawableRes val iconResource: Int,
    val lightThemeIconUrl: String?,
    val darkThemeIconUrl: String?,
    val iconRequiresTinting: Boolean,
    val promoBadge: String?,
)
