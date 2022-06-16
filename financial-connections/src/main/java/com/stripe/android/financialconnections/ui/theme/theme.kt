package com.stripe.android.financialconnections.ui.theme

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColorPalette = FinancialConnectionsColors(
    backgroundSurface = Color.White,
    backgroundContainer = Neutral50,
    textPrimary = Neutral800,
    textSecondary = Neutral500,
    textDisabled = Neutral300,
    textWhite = Color.White,
    textBrand = Blurple500,
    textInfo = Blue500,
    textSuccess = Color.Unspecified,
    textAttention = Color.Unspecified,
    textCritical = Color.Unspecified
)

private val Typography = FinancialConnectionsTypography(
    subtitle = TextStyle(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.W700
    ),
    subtitleEmphasized = TextStyle(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.W700
    ),
    heading = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.W600
    ),
    subheading = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.W600
    ),
    kicker = TextStyle(
        // TODO caps.
        fontSize = 12.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.W600,
    ),
    body = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.W400,
    ),
    bodyEmphasized = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.W600,
    ),
    detail = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.W400,
    ),
    detailEmphasized = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.W600,
    ),
    caption = TextStyle(
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.W400,
    ),
    captionEmphasized = TextStyle(
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.W600,
    ),
    captionTight = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.W400,
    ),
    captionTightEmphasized = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.W600,
    ),
)

@Composable
fun FinancialConnectionsTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalFinancialConnectionsTypography provides Typography,
        LocalFinancialConnectionsColors provides LightColorPalette,
    ) {
        MaterialTheme(
            colors = debugColors(),
            content = content
        )
    }
}

val LocalFinancialConnectionsTypography = staticCompositionLocalOf<FinancialConnectionsTypography> {
    error("no FinancialConnectionsTypography provided")
}

val LocalFinancialConnectionsColors = staticCompositionLocalOf<FinancialConnectionsColors> {
    error("No FinancialConnectionsColors provided")
}

object FinancialConnectionsTheme {
    val colors: FinancialConnectionsColors
        @Composable
        get() = LocalFinancialConnectionsColors.current
    val typography: FinancialConnectionsTypography
        @Composable
        get() = LocalFinancialConnectionsTypography.current
}

/**
 * A Material [Colors] implementation which sets all colors to [debugColor] to discourage usage of
 * [MaterialTheme.colors] in preference to [FinancialConnectionsColors].
 */
fun debugColors(
    debugColor: Color = Color.Magenta
) = Colors(
    primary = debugColor,
    primaryVariant = debugColor,
    secondary = debugColor,
    secondaryVariant = debugColor,
    background = debugColor,
    surface = debugColor,
    error = debugColor,
    onPrimary = debugColor,
    onSecondary = debugColor,
    onBackground = debugColor,
    onSurface = debugColor,
    onError = debugColor,
    isLight = true
)
