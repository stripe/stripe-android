package com.stripe.android.link

import androidx.compose.ui.graphics.Color

object LinkAppearanceFixtures {
    val YELLOW_COLORS = LinkAppearance.Colors(
        primary = Color.Yellow,
        contentOnPrimary = Color.DarkGray,
        borderSelected = Color.Yellow,
    )
    val GRAY_COLORS = LinkAppearance.Colors(
        primary = Color.DarkGray,
        contentOnPrimary = Color.White,
        borderSelected = Color.DarkGray,
    )
    val TALL_RECTANGLE_BUTTON = LinkAppearance.PrimaryButton(
        cornerRadiusDp = 0f,
        heightDp = 64f
    )
    val DEFAULT = LinkAppearance(
        lightColors = YELLOW_COLORS,
        darkColors = GRAY_COLORS,
        style = LinkAppearance.Style.AUTOMATIC,
        primaryButton = TALL_RECTANGLE_BUTTON,
    )
    val ALWAYS_DARK = LinkAppearance(
        lightColors = YELLOW_COLORS,
        darkColors = GRAY_COLORS,
        style = LinkAppearance.Style.ALWAYS_DARK
    )
}
