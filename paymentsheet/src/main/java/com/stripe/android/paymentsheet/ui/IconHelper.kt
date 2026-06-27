package com.stripe.android.paymentsheet.ui

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import com.stripe.android.uicore.IconStyle
import com.stripe.android.uicore.LocalIconStyle
import com.stripe.android.uicore.isComponentColorDark

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
        return if (isComponentColorDark() && darkThemeIconUrl != null) darkThemeIconUrl else lightThemeIconUrl
    }

    @Composable
    @DrawableRes
    private fun iconForTheme(iconRes: Int, iconResNight: Int?): Int {
        return if (isComponentColorDark() && iconResNight != null) iconResNight else iconRes
    }
}
