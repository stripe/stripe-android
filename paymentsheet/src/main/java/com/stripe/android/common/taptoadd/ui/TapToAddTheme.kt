package com.stripe.android.common.taptoadd.ui

import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.stripe.android.uicore.StripeColors
import com.stripe.android.uicore.StripeTheme

@Composable
internal fun TapToAddTheme(
    content: @Composable () -> Unit,
) {
    StripeTheme(
        colors = TapToAddThemeDefaults.colors,
    ) {
        content()
    }
}

private object TapToAddThemeDefaults {
    private val componentBorderColor = Color(0xFF48484A)

    val colors = StripeColors(
        component = Color.Black,
        componentBorder = componentBorderColor,
        componentDivider = componentBorderColor,
        onComponent = Color.White,
        subtitle = Color.White,
        textCursor = Color.White,
        placeholderText = Color.White,
        appBarIcon = Color.White,
        materialColors = darkColors(
            primary = Color.White,
            primaryVariant = Color(0xFF7A7A7A),
            background = Color.Black,
            onBackground = Color(0xFFE3E3E3),
            surface = Color.Black,
            onSurface = Color(0xFFE3E3E3),
            error = Color.Red,
        ),
    )
}
