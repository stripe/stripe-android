package com.stripe.android.lpmfoundations

import androidx.annotation.DrawableRes
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.uicore.IconStyle

internal data class FormHeaderInformation(
    val displayName: ResolvableString,
    val shouldShowIcon: Boolean,
    @DrawableRes private val iconResource: Int,
    val lightThemeIconUrl: String?,
    val darkThemeIconUrl: String?,
    val iconRequiresTinting: Boolean,
    val promoBadge: String?,
    private val outlinedIconResource: Int? = null,
) {
    fun icon(style: IconStyle) = when (style) {
        IconStyle.Filled -> iconResource
        IconStyle.Outlined -> outlinedIconResource ?: iconResource
    }
}
