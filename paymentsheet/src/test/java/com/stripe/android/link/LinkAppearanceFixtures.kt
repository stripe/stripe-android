package com.stripe.android.link

import androidx.compose.ui.graphics.Color

object LinkAppearanceFixtures {
    val LIGHT_COLORS = LinkAppearance.Colors(
        primary = Color.Yellow,
        contentOnPrimary = Color.DarkGray,
        borderSelected = Color.Yellow,
    )
    val DARK_COLORS = LinkAppearance.Colors(
        primary = Color.DarkGray,
        contentOnPrimary = Color.White,
        borderSelected = Color.DarkGray,
    )
    val DEFAULT = LinkAppearance(
        lightColors = LIGHT_COLORS,
        darkColors = DARK_COLORS,
        style = LinkAppearance.Style.AUTOMATIC
    )
    val ALWAYS_DARK = LinkAppearance(
        lightColors = LIGHT_COLORS,
        darkColors = DARK_COLORS,
        style = LinkAppearance.Style.ALWAYS_DARK
    )
}
