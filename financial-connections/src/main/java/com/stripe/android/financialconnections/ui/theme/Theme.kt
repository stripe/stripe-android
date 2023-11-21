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
import androidx.navigation.compose.rememberNavController
import com.stripe.android.financialconnections.ui.LocalNavHostController

@Deprecated("Use V3Colors instead")
private val Colors = FinancialConnectionsColors(
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

private val V3Colors = FinancialConnectionsV3Colors(
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
    backgroundBrand = Color(0xFFF5F6F8),
    border = Color(0xFFD8DEE4),
    borderBrand = Color(0xFF675DFF)
)

@Deprecated("Use V3Typography instead")
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

private val V3Typography = FinancialConnectionsV3Typography(
    headingXLarge = TextStyle(
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.38.sp,
        fontWeight = FontWeight.W700
    ),
    headingLarge = TextStyle(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.30.sp,
        fontWeight = FontWeight.W700
    ),
    headingMedium = TextStyle(
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.30.sp,
        fontWeight = FontWeight.W700
    ),
    bodyMediumEmphasized = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.W600
    ),
    bodyMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.W400
    ),
    bodySmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.W400
    ),
    labelLargeEmphasized = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.W600
    ),
    labelLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.W400
    ),
    labelMediumEmphasized = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.W600
    ),
    labelMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.W400
    ),
    labelSmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.W400
    ),
)

private val TextSelectionColors = TextSelectionColors(
    handleColor = V3Colors.textBrand,
    backgroundColor = V3Colors.textBrand.copy(alpha = 0.4f)
)

@Immutable
private object FinancialConnectionsRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor() = RippleTheme.defaultRippleColor(
        contentColor = V3Colors.textBrand,
        lightTheme = MaterialTheme.colors.isLight
    )

    @Composable
    override fun rippleAlpha() = RippleTheme.defaultRippleAlpha(
        contentColor = V3Colors.textBrand,
        lightTheme = MaterialTheme.colors.isLight
    )
}

@Composable
internal fun FinancialConnectionsTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalFinancialConnectionsTypography provides Typography,
        LocalNavHostController provides rememberNavController(),
        LocalV3Typography provides V3Typography,
        LocalFinancialConnectionsColors provides Colors,
        LocalV3Colors provides V3Colors
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

private val LocalV3Typography =
    staticCompositionLocalOf<FinancialConnectionsV3Typography> {
        error("no V3Typography provided")
    }

private val LocalV3Colors =
    staticCompositionLocalOf<FinancialConnectionsV3Colors> {
        error("no V3Colors provided")
    }

private val LocalFinancialConnectionsColors = staticCompositionLocalOf<FinancialConnectionsColors> {
    error("No FinancialConnectionsColors provided")
}

internal object FinancialConnectionsTheme {

    @Deprecated("Use v3Colors instead")
    val colors: FinancialConnectionsColors
        @Composable
        get() = LocalFinancialConnectionsColors.current

    @Deprecated("Use v3Typography instead")
    val typography: FinancialConnectionsTypography
        @Composable
        get() = LocalFinancialConnectionsTypography.current
    val v3Typography
        @Composable
        get() = LocalV3Typography.current
    val v3Colors
        @Composable
        get() = LocalV3Colors.current
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
