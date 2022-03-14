package com.stripe.android.ui.core

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.annotation.RestrictTo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val LocalFieldTextStyle = TextStyle.Default.copy(
    fontFamily = FontFamily.SansSerif,
    fontSize = 14.sp
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaymentsColors(
    val primary: Color,
    val surface: Color,
    val componentBackground: Color,
    val componentBorder: Color,
    val componentDivider: Color,
    val onPrimary: Color,
    val textSecondary: Color,
    val placeholderText: Color,
    val onBackground: Color,
    val appBarIcon: Color,
    val error: Color,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object PaymentsThemeConfig {
    fun colors(isDark: Boolean): PaymentsColors {
        return if (isDark) colorsDark else colorsLight
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object Shapes {
        val cornerRadius = 6.dp
        val borderStrokeWidth = 1.dp
        val borderStrokeWidthSelected = 2.dp
    }

    private val colorsLight = PaymentsColors(
        primary = Color(0xFF007AFF),
        surface = Color.White,
        componentBackground = Color.White,
        componentBorder = Color(0x33787880),
        componentDivider = Color(0x33787880),
        onPrimary = Color.Black,
        textSecondary = Color(0x99000000),
        placeholderText = Color(0x993C3C43),
        onBackground = Color.Black,
        appBarIcon = Color(0x99000000),
        error = Color.Red,
    )

    private val colorsDark = PaymentsColors(
        primary = Color(0xFF0074D4),
        surface = Color(0xff2e2e2e),
        componentBackground = Color.DarkGray,
        componentBorder = Color(0xFF787880),
        componentDivider = Color(0xFF787880),
        onPrimary = Color.White,
        textSecondary = Color(0x99FFFFFF),
        placeholderText = Color(0x61FFFFFF),
        onBackground = Color.White,
        appBarIcon = Color.White,
        error = Color.Red,
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaymentsComposeColors(
    val colorComponentBackground: Color,
    val colorComponentBorder: Color,
    val colorComponentDivider: Color,
    val colorTextSecondary: Color,
    val placeholderText: Color,
    val material: Colors
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaymentsComposeShapes(
    val borderStrokeWidth: Dp,
    val borderStrokeWidthSelected: Dp,
    val material: Shapes
)

@Composable
@ReadOnlyComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun PaymentsThemeConfig.toComposeColors(): PaymentsComposeColors {
    val colors = colors(isSystemInDarkTheme())
    return PaymentsComposeColors(
        colorComponentBackground = colors.componentBackground,
        colorComponentBorder = colors.componentBorder,
        colorComponentDivider = colors.componentDivider,
        colorTextSecondary = colors.textSecondary,
        placeholderText = colors.placeholderText,

        material = lightColors(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            surface = colors.surface,
            onBackground = colors.onBackground,
            error = colors.error,
        )
    )
}

@Composable
@ReadOnlyComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun PaymentsThemeConfig.Shapes.toComposeShapes(): PaymentsComposeShapes {
    return PaymentsComposeShapes(
        borderStrokeWidth = borderStrokeWidth,
        borderStrokeWidthSelected = borderStrokeWidthSelected,
        material = MaterialTheme.shapes.copy(
            small = RoundedCornerShape(cornerRadius),
            medium = RoundedCornerShape(cornerRadius)
        )
    )
}

@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun PaymentsTheme(
    content: @Composable () -> Unit
) {
    val colors = PaymentsThemeConfig.toComposeColors()
    val localColors = staticCompositionLocalOf { colors }

    val shapes = PaymentsThemeConfig.Shapes.toComposeShapes()
    val localShapes = staticCompositionLocalOf { shapes }

    CompositionLocalProvider(
        localColors provides colors,
        localShapes provides shapes
    ) {
        MaterialTheme(
            colors = PaymentsTheme.colors.material,
            typography = MaterialTheme.typography.copy(
                body1 = LocalFieldTextStyle,
                subtitle1 = LocalFieldTextStyle
            ),
            shapes = PaymentsTheme.shapes.material,
            content = content
        )
    }
}

// This object lets you access colors in composables via
// StripeTheme.colors.primary etc
// This mirrors an object that lives inside of MaterialTheme.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object PaymentsTheme {
    val colors: PaymentsComposeColors
        @Composable
        @ReadOnlyComposable
        get() = PaymentsThemeConfig.toComposeColors()

    val shapes: PaymentsComposeShapes
        @Composable
        @ReadOnlyComposable
        get() = PaymentsThemeConfig.Shapes.toComposeShapes()

    @Composable
    @ReadOnlyComposable
    fun getBorderStroke(isSelected: Boolean): BorderStroke {
        val width = if (isSelected) shapes.borderStrokeWidthSelected else shapes.borderStrokeWidth
        val color = if (isSelected) colors.material.primary else colors.colorComponentBorder
        return BorderStroke(width, color)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Context.isSystemDarkTheme(): Boolean {
    return resources.configuration.uiMode and
        UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES
}
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Context.convertDpToPx(dp: Dp): Float {
    return dp.value * resources.displayMetrics.density
}
