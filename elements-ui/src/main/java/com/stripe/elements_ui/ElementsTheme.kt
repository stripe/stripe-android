package com.stripe.elements_ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ElementColors(
    val primary: Color,
    val surface: Color,
    val onSurface: Color,
    val subtitle: Color,
    val component: Color,
    val placeholder: Color,
    val error: Color,
    val appBarIcon: Color,
    val componentBorder: Color,
    val componentDivider: Color,
    val onComponent: Color,
)

val LocalElementColors = staticCompositionLocalOf {
    ElementColors(

    )
}

object ElementsTheme {
    val colors: ElementColors
        @Composable
        get() = LocalElementColors.current

}