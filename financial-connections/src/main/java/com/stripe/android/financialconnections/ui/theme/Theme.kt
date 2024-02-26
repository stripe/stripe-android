package com.stripe.android.financialconnections.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.Colors
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.stripe.android.financialconnections.ui.LocalNavHostController

private val Colors = FinancialConnectionsColors(
    textDefault = Color(0xFF353A44),
    textSubdued = Color(0xFF596171),
    textDisabled = Color(0xFF818DA0),
    textWhite = Color(0xFFFFFFFF),
    textBrand = Color(0xFF533AFD),
    textCritical = Color(0xFFC0123C),
    iconDefault = Color(0xFF474E5A),
    iconSubdued = Color(0xFF6C7688),
    iconWhite = Color(0xFFFFFFFF),
    iconBrand = Color(0xFF675DFF),
    buttonPrimary = Color(0xFF675DFF),
    buttonPrimaryHover = Color(0xFF857AFE),
    buttonPrimaryPressed = Color(0xFF533AFD),
    buttonSecondary = Color(0xFFF5F6F8),
    buttonSecondaryHover = Color(0xFFF5F6F8),
    buttonSecondaryPressed = Color(0xFFEBEEF1),
    background = Color(0xFFF5F6F8),
    backgroundSurface = Color(0xFFFFFFFF),
    backgroundOffset = Color(0xFFF6F8FA),
    backgroundBrand = Color(0xFFF5F6F8),
    border = Color(0xFFD8DEE4),
    borderBrand = Color(0xFF675DFF)
)

private val lineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

private val Typography = FinancialConnectionsTypography(
    headingXLarge = TextStyle(
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.38.sp,
        fontWeight = FontWeight.W700,
        lineHeightStyle = lineHeightStyle
    ).toCompat(),
    headingXLargeSubdued = TextStyle(
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.38.sp,
        fontWeight = FontWeight.W400,
        lineHeightStyle = lineHeightStyle
    ).toCompat(),
    headingLarge = TextStyle(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.30.sp,
        fontWeight = FontWeight.W700,
        lineHeightStyle = lineHeightStyle
    ).toCompat(),
    headingMedium = TextStyle(
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.30.sp,
        fontWeight = FontWeight.W700,
        lineHeightStyle = lineHeightStyle
    ).toCompat(),
    bodyMediumEmphasized = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.W600,
        lineHeightStyle = lineHeightStyle
    ).toCompat(),
    bodyMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.W400,
        lineHeightStyle = lineHeightStyle
    ).toCompat(),
    bodySmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.W400,
        lineHeightStyle = lineHeightStyle
    ).toCompat(),
    labelLargeEmphasized = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.W600,
        lineHeightStyle = lineHeightStyle
    ).toCompat(),
    labelLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.W400,
        lineHeightStyle = lineHeightStyle
    ).toCompat(),
    labelMediumEmphasized = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.W600,
        lineHeightStyle = lineHeightStyle
    ).toCompat(),
    labelMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.W400,
        lineHeightStyle = lineHeightStyle
    ).toCompat(),
    labelSmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.W400,
        lineHeightStyle = lineHeightStyle
    ).toCompat(),
)

private val TextSelectionColors = TextSelectionColors(
    handleColor = Colors.textBrand,
    backgroundColor = Colors.textBrand.copy(alpha = 0.4f)
)

@Immutable
private object FinancialConnectionsRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor() = RippleTheme.defaultRippleColor(
        contentColor = Colors.textBrand,
        lightTheme = MaterialTheme.colors.isLight
    )

    @Composable
    override fun rippleAlpha() = RippleTheme.defaultRippleAlpha(
        contentColor = Colors.textBrand,
        lightTheme = MaterialTheme.colors.isLight
    )
}

@Composable
internal fun FinancialConnectionsTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalNavHostController provides rememberNavController(),
        LocalTypography provides Typography,
        LocalColors provides Colors
    ) {
        val view = LocalView.current
        val window = findWindow()
        val barColor = FinancialConnectionsTheme.colors.border
        if (!view.isInEditMode) {
            SideEffect {
                window?.let { window ->
                    val insets = WindowCompat.getInsetsController(window, view)
                    window.statusBarColor = barColor.toArgb()
                    window.navigationBarColor = barColor.toArgb()
                    insets.isAppearanceLightStatusBars = true
                    insets.isAppearanceLightNavigationBars = true
                }
            }
        }
        MaterialTheme(
            colors = debugColors(),
            content = {
                CompositionLocalProvider(
                    LocalTextSelectionColors provides TextSelectionColors,
                    LocalTextStyle provides LocalTextStyle.current.toCompat(useDefaultLineHeight = true),
                    LocalRippleTheme provides FinancialConnectionsRippleTheme
                ) {
                    content()
                }
            }
        )
    }
}

@Composable
private fun findWindow(): Window? =
    (LocalView.current.parent as? DialogWindowProvider)?.window
        ?: LocalView.current.context.findWindow()

private tailrec fun Context.findWindow(): Window? =
    when (this) {
        is Activity -> window
        is ContextWrapper -> baseContext.findWindow()
        else -> null
    }

private val LocalTypography =
    staticCompositionLocalOf<FinancialConnectionsTypography> {
        error("no Typography provided")
    }

private val LocalColors =
    staticCompositionLocalOf<FinancialConnectionsColors> {
        error("no Colors provided")
    }

internal object FinancialConnectionsTheme {
    val typography
        @Composable
        get() = LocalTypography.current
    val colors
        @Composable
        get() = LocalColors.current
}

private fun TextStyle.toCompat(useDefaultLineHeight: Boolean = false): TextStyle {
    return copy(
        lineHeight = if (useDefaultLineHeight) {
            TextStyle.Default.lineHeight
        } else {
            lineHeight
        },
        lineHeightStyle = TextStyle.Default.lineHeightStyle,
        platformStyle = PlatformTextStyle(includeFontPadding = true),
    )
}

/**
 * A Material [Colors] implementation which sets all colors to [debugColor] to discourage usage of
 * [MaterialTheme.colors] in preference to [FinancialConnectionsColors].
 */
private fun debugColors(
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
