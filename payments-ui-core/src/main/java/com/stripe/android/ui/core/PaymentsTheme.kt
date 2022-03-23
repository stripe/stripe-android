package com.stripe.android.ui.core

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.annotation.RestrictTo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaymentsComposeColors(
    val colorComponentBackground: Color,
    val colorComponentBorder: Color,
    val colorComponentDivider: Color,
    val colorTextSecondary: Color,
    val colorTextCursor: Color,
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun PaymentsTheme(
    content: @Composable () -> Unit
) {
    val colors = PaymentsTheme.colors
    val localColors = staticCompositionLocalOf { colors }

    val shapes = PaymentsTheme.shapes
    val localShapes = staticCompositionLocalOf { shapes }

    CompositionLocalProvider(
        localColors provides colors,
        localShapes provides shapes
    ) {
        MaterialTheme(
            colors = PaymentsTheme.colors.material,
            typography = PaymentsTheme.typography,
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
    lateinit var colorsLight: PaymentsComposeColors
    lateinit var colorsDark: PaymentsComposeColors
    val colors: PaymentsComposeColors
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) colorsDark else colorsLight

    lateinit var shapes: PaymentsComposeShapes

    lateinit var typography: Typography
    const val xxsmallFont = 9.0
    const val xsmallFont = 12.0
    const val smallFont = 13.0
    const val mediumFont = 14.0
    const val largeFont = 16.0
    const val extraLargeFont = 20.0

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
