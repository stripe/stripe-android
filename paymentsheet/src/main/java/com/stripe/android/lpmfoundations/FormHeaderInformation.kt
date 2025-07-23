package com.stripe.android.lpmfoundations

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.paymentsheet.ui.IconHelper
import com.stripe.android.uicore.IconStyle

internal data class FormHeaderInformation(
    val displayName: ResolvableString,
    val shouldShowIcon: Boolean,
    @DrawableRes private val iconResource: Int,
    @DrawableRes val iconResourceNight: Int? = null,
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

    @Composable
    fun icon() = IconHelper.icon(
        iconRes = iconResource,
        iconResNight = iconResourceNight,
        outlinedIconResource = outlinedIconResource
    )

    @Composable
    fun iconUrl() = IconHelper.iconUrl(
        lightThemeIconUrl = lightThemeIconUrl,
        darkThemeIconUrl = darkThemeIconUrl
    )
}