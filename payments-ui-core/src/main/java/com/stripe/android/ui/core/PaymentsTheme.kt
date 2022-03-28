package com.stripe.android.ui.core

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.annotation.FontRes
import androidx.annotation.RestrictTo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaymentsColors(
    val primary: Color,
    val surface: Color,
    val componentBackground: Color,
    val componentBorder: Color,
    val componentDivider: Color,
    val onPrimary: Color,
    val textSecondary: Color,
    val textCursor: Color,
    val placeholderText: Color,
    val onBackground: Color,
    val appBarIcon: Color,
    val error: Color,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaymentsShapes(
    val cornerRadius: Float,
    val borderStrokeWidth: Float,
    val borderStrokeWidthSelected: Float,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaymentsTypography(
    val fontWeightNormal: Int,
    val fontWeightMedium: Int,
    val fontWeightBold: Int,
    val fontSizeMultiplier: Float,
    val xxSmallFontSize: TextUnit,
    val xSmallFontSize: TextUnit,
    val smallFontSize: TextUnit,
    val mediumFontSize: TextUnit,
    val largeFontSize: TextUnit,
    val xLargeFontSize: TextUnit,
    @FontRes
    val fontFamily: Int
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object PaymentsThemeDefaults {
    fun colors(isDark: Boolean): PaymentsColors {
        return if (isDark) colorsDark else colorsLight
    }

    val colorsLight = PaymentsColors(
        primary = Color(0xFF007AFF),
        surface = Color.White,
        componentBackground = Color.White,
        componentBorder = Color(0x33787880),
        componentDivider = Color(0x33787880),
        onPrimary = Color.Black,
        textSecondary = Color(0x99000000),
        textCursor = Color.Black,
        placeholderText = Color(0x993C3C43),
        onBackground = Color.Black,
        appBarIcon = Color(0x99000000),
        error = Color.Red,
    )

    val colorsDark = PaymentsColors(
        primary = Color(0xFF0074D4),
        surface = Color(0xff2e2e2e),
        componentBackground = Color.DarkGray,
        componentBorder = Color(0xFF787880),
        componentDivider = Color(0xFF787880),
        onPrimary = Color.White,
        textSecondary = Color(0x99FFFFFF),
        textCursor = Color.White,
        placeholderText = Color(0x61FFFFFF),
        onBackground = Color.White,
        appBarIcon = Color.White,
        error = Color.Red,
    )

    val shapes = PaymentsShapes(
        cornerRadius = 6.0f,
        borderStrokeWidth = 1.0f,
        borderStrokeWidthSelected = 2.0f
    )

    val typography = PaymentsTypography(
        fontWeightNormal = FontWeight.Normal.weight,
        fontWeightMedium = FontWeight.Medium.weight,
        fontWeightBold = FontWeight.Bold.weight,
        fontSizeMultiplier = 1.0F,
        xxSmallFontSize = 9.sp,
        xSmallFontSize = 12.sp,
        smallFontSize = 13.sp,
        mediumFontSize = 14.sp,
        largeFontSize = 16.sp,
        xLargeFontSize = 20.sp,
        fontFamily = R.font.roboto
    )
}

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
@ReadOnlyComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun PaymentsColors.toComposeColors(): PaymentsComposeColors {
    return PaymentsComposeColors(
        colorComponentBackground = componentBackground,
        colorComponentBorder = componentBorder,
        colorComponentDivider = componentDivider,
        colorTextSecondary = textSecondary,
        colorTextCursor = textCursor,
        placeholderText = placeholderText,

        material = lightColors(
            primary = primary,
            onPrimary = onPrimary,
            surface = surface,
            onBackground = onBackground,
            error = error,
        )
    )
}

@Composable
@ReadOnlyComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun PaymentsShapes.toComposeShapes(): PaymentsComposeShapes {
    return PaymentsComposeShapes(
        borderStrokeWidth = borderStrokeWidth.dp,
        borderStrokeWidthSelected = borderStrokeWidthSelected.dp,
        material = MaterialTheme.shapes.copy(
            small = RoundedCornerShape(cornerRadius.dp),
            medium = RoundedCornerShape(cornerRadius.dp)
        )
    )
}

@Composable
@ReadOnlyComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun PaymentsTypography.toComposeTypography(): Typography {
    // h4 is our largest headline. It is used for the most important labels in our UI
    // ex: "Select your payment method" in Payment Sheet.
    val h4 = TextStyle.Default.copy(
        fontFamily = FontFamily(Font(fontFamily)),
        fontSize = (xLargeFontSize * fontSizeMultiplier),
        fontWeight = FontWeight(fontWeightBold),
    )

    // h5 is our medium headline label.
    // ex: "Pay $50.99" in Payment Sheet's buy button.
    val h5 = TextStyle.Default.copy(
        fontFamily = FontFamily(Font(fontFamily)),
        fontSize = (largeFontSize * fontSizeMultiplier),
        fontWeight = FontWeight(fontWeightMedium),
        letterSpacing = (-0.32).sp
    )

    // h6 is our smallest headline label.
    // ex: Section labels in Payment Sheet
    val h6 = TextStyle.Default.copy(
        fontFamily = FontFamily(Font(fontFamily)),
        fontSize = (smallFontSize * fontSizeMultiplier),
        fontWeight = FontWeight(fontWeightMedium),
        letterSpacing = (-0.15).sp
    )

    // body1 is our larger body text. Used for the bulk of our elements and forms.
    // ex: the text used in Payment Sheet's text form elements.
    val body1 = TextStyle.Default.copy(
        fontFamily = FontFamily(Font(fontFamily)),
        fontSize = (mediumFontSize * fontSizeMultiplier),
        fontWeight = FontWeight(fontWeightNormal),
    )

    // subtitle1 is our only subtitle size. Used for labeling fields.
    // ex: the placeholder texts that appear when you type in Payment Sheet's forms.
    val subtitle1 = TextStyle.Default.copy(
        fontFamily = FontFamily(Font(fontFamily)),
        fontSize = (mediumFontSize * fontSizeMultiplier),
        fontWeight = FontWeight(fontWeightNormal),
        letterSpacing = (-0.15).sp
    )

    // caption is used to label images in payment sheet.
    // ex: the labels under our payment method selectors in Payment Sheet.
    val caption = TextStyle.Default.copy(
        fontFamily = FontFamily(Font(fontFamily)),
        fontSize = (xSmallFontSize * fontSizeMultiplier),
        fontWeight = FontWeight(fontWeightMedium)
    )

    // body2 is our smaller body text. Used for less important fields that are not required to
    // read. Ex: our mandate texts in Payment Sheet.
    val body2 = TextStyle.Default.copy(
        fontFamily = FontFamily(Font(fontFamily)),
        fontSize = (xxSmallFontSize * fontSizeMultiplier),
        fontWeight = FontWeight(fontWeightNormal),
        letterSpacing = (-0.15).sp
    )

    return MaterialTheme.typography.copy(
        body1 = body1,
        body2 = body2,
        h4 = h4,
        h5 = h5,
        h6 = h6,
        subtitle1 = subtitle1,
        caption = caption
    )
}

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
            colors = colors.material,
            typography = PaymentsTheme.typography,
            shapes = shapes.material,
            content = content
        )
    }
}

// This object lets you access colors in composables via
// StripeTheme.colors.primary etc
// This mirrors an object that lives inside of MaterialTheme.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object PaymentsTheme {
    var colorsDarkMutable = PaymentsThemeDefaults.colorsDark
    var colorsLightMutable = PaymentsThemeDefaults.colorsLight
    val colors: PaymentsComposeColors
        @Composable
        @ReadOnlyComposable
        get() = (if (isSystemInDarkTheme()) colorsDarkMutable else colorsLightMutable).toComposeColors()

    var shapesMutable = PaymentsThemeDefaults.shapes
    val shapes: PaymentsComposeShapes
        @Composable
        @ReadOnlyComposable
        get() = shapesMutable.toComposeShapes()

    var typographyMutable = PaymentsThemeDefaults.typography
    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = typographyMutable.toComposeTypography()

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
