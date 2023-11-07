package com.stripe.elements_ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ElementColors(
    val primary: Color,
    val surface: Color,
    val onPrimary: Color,
    val onSurface: Color,
    val subtitle: Color,
    val component: Color,
    val placeholder: Color,
    val error: Color,
    val textCursor: Color,
    val appBarIcon: Color,
    val componentBorder: Color,
    val componentDivider: Color,
    val onComponent: Color,
) {

    fun toMaterialColors(): Colors {
        return lightColors(
            primary = primary,
            surface = surface,
            onSurface = onSurface,
            onPrimary = onPrimary,
            error = error,
        )
    }
}

val LocalElementColors = staticCompositionLocalOf<ElementColors> {
    error("No ElementColors provided")
}

object ElementsTheme {
    val colors: ElementColors
        @Composable
        get() = LocalElementColors.current
}

@Composable
fun ElementsTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = ElementColors(
        primary = Color(0xFF007AFF),
        surface = Color.White,
        onSurface = Color.Black,
        onPrimary = Color.White,
        component = Color.White,
        componentBorder = Color(0x33787880),
        componentDivider = Color(0x33787880),
        onComponent = Color.Black,
        error = Color.Red,
        subtitle = Color(0x99000000),
        textCursor = Color.Black,
        placeholder = Color(0x993C3C43),
        appBarIcon = Color(0x99000000),
    )

    CompositionLocalProvider(
        LocalElementColors provides colors,
    ) {
        MaterialTheme(
            colors = colors.toMaterialColors(),
        ) {
            content()
        }
    }
}
