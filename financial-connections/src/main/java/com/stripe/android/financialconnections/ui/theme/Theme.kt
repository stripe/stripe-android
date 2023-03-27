package com.stripe.android.financialconnections.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.Colors
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

private val LightColorPalette = FinancialConnectionsColors(
    backgroundSurface = Color.White,
    backgroundContainer = Neutral50,
    backgroundBackdrop = Neutral200.copy(alpha = .70f),
    borderDefault = Neutral150,
    borderFocus = Blue400.copy(alpha = .36f),
    borderInvalid = Red500,
    textPrimary = Neutral800,
    textSecondary = Neutral500,
    textDisabled = Neutral300,
    textWhite = Color.White,
    textBrand = Brand500,
    textInfo = Blue500,
    textSuccess = Green500,
    textAttention = Attention500,
    textCritical = Red500,
    iconBrand = Brand400,
    iconInfo = Blue400,
    iconSuccess = Green400,
    iconAttention = Attention400
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
        fontWeight = FontWeight.W700
    ),
    subheading = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.W600
    ),
    kicker = TextStyle(
        fontSize = 12.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.W600
    ),
    body = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.W400
    ),
    bodyEmphasized = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.W600
    ),
    detail = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.W400
    ),
    detailEmphasized = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.W600
    ),
    caption = TextStyle(
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.W400
    ),
    captionEmphasized = TextStyle(
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.W600
    ),
    captionTight = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.W400
    ),
    captionTightEmphasized = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.W600
    ),
    bodyCode = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.W400
    ),
    bodyCodeEmphasized = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.W700
    ),
    captionCode = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.W400
    ),
    captionCodeEmphasized = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.W700
    ),
)

private val TextSelectionColors = TextSelectionColors(
    handleColor = LightColorPalette.textBrand,
    backgroundColor = LightColorPalette.textBrand.copy(alpha = 0.4f)
)

@Immutable
private object FinancialConnectionsRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor() = RippleTheme.defaultRippleColor(
        contentColor = LightColorPalette.textBrand,
        lightTheme = MaterialTheme.colors.isLight
    )

    @Composable
    override fun rippleAlpha() = RippleTheme.defaultRippleAlpha(
        contentColor = LightColorPalette.textBrand,
        lightTheme = MaterialTheme.colors.isLight
    )
}

@Composable
internal fun FinancialConnectionsTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalFinancialConnectionsTypography provides Typography,
        LocalFinancialConnectionsColors provides LightColorPalette
    ) {
        val view = LocalView.current
        val window = findWindow()
        val barColor = FinancialConnectionsTheme.colors.borderDefault
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

private val LocalFinancialConnectionsTypography =
    staticCompositionLocalOf<FinancialConnectionsTypography> {
        error("no FinancialConnectionsTypography provided")
    }

private val LocalFinancialConnectionsColors = staticCompositionLocalOf<FinancialConnectionsColors> {
    error("No FinancialConnectionsColors provided")
}

internal object FinancialConnectionsTheme {
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
