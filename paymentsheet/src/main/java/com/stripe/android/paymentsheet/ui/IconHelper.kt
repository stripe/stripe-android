package com.stripe.android.paymentsheet.ui

import androidx.annotation.DrawableRes
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
import com.stripe.android.uicore.IconStyle
import com.stripe.android.uicore.LocalIconStyle
import com.stripe.android.uicore.stripeColors

private const val MIN_LUMINANCE_FOR_LIGHT_ICON = 0.5

internal object IconHelper {
    @Composable
    @DrawableRes
    fun icon(iconRes: Int, iconResNight: Int?, outlinedIconResource: Int?): Int {
        val filledIcon = iconForTheme(iconRes, iconResNight)
        return when (LocalIconStyle.current) {
            IconStyle.Filled -> filledIcon
            IconStyle.Outlined -> outlinedIconResource ?: filledIcon
        }
    }

    @Composable
    fun iconUrl(lightThemeIconUrl: String?, darkThemeIconUrl: String?): String? {
        return if (isDark() && darkThemeIconUrl != null) darkThemeIconUrl else lightThemeIconUrl
    }

    @Composable
    fun isDark(): Boolean {
        val color = MaterialTheme.stripeColors.component
        return color.luminance() < MIN_LUMINANCE_FOR_LIGHT_ICON
    }

    @Composable
    @DrawableRes
    private fun iconForTheme(iconRes: Int, iconResNight: Int?): Int {
        return if (isDark() && iconResNight != null) iconResNight else iconRes
    }
}
