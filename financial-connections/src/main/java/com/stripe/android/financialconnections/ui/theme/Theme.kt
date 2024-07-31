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
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.ui.LocalNavHostController
import com.stripe.android.uicore.R as StripeUiCoreR

internal enum class Theme {
    DefaultLight,
    LinkLight;

    val colors: FinancialConnectionsColors
        get() = when (this) {
            DefaultLight -> Colors
            LinkLight -> InstantDebitsColors
        }

    val icon: Int
        get() = when (this) {
            DefaultLight -> R.drawable.stripe_logo
            LinkLight -> StripeUiCoreR.drawable.stripe_link_logo_bw
        }

    companion object {
        val default: Theme = DefaultLight
    }
}

private val Colors = object : FinancialConnectionsColors {
    override val textDefault = Neutral800
    override val textSubdued = Neutral600
    override val textDisabled = Neutral300
    override val textWhite = Neutral0
    override val textBrand = Brand600
    override val textCritical = Critical500
    override val iconDefault = Neutral700
    override val iconWhite = Neutral0
    override val iconBrand = Brand500
    override val iconCaution = Attention300
    override val buttonPrimary = Brand500
    override val buttonSecondary = Neutral25
    override val background = Neutral25
    override val backgroundSurface = Neutral0
    override val backgroundOffset = Neutral50
    override val backgroundBrand = Neutral25
    override val backgroundCaution = Attention50
    override val border = Neutral100
    override val borderBrand = Brand500
    override val contentOnBrand = Neutral0
}

private val InstantDebitsColors = object : FinancialConnectionsColors {
    override val textDefault = Neutral800
    override val textSubdued = Neutral600
    override val textDisabled = Neutral300
    override val textWhite = Neutral0
    override val textBrand = LinkGreen500
    override val textCritical = Critical500
    override val iconDefault = Neutral700
    override val iconWhite = Neutral0
    override val iconBrand = LinkGreen500
    override val iconCaution = Attention300
    override val buttonPrimary = LinkGreen200
    override val buttonSecondary = Neutral25
    override val background = Neutral25
    override val backgroundSurface = Neutral0
    override val backgroundOffset = Neutral50
    override val backgroundBrand = Neutral25
    override val backgroundCaution = Attention50
    override val border = Neutral100
    override val borderBrand = LinkGreen200
    override val contentOnBrand = LinkGreen900
}

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

private val TextSelectionColors: TextSelectionColors
    @Composable
    get() = TextSelectionColors(
        handleColor = FinancialConnectionsTheme.colors.textBrand,
        backgroundColor = FinancialConnectionsTheme.colors.textBrand.copy(alpha = 0.4f)
    )

@Immutable
private object FinancialConnectionsRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor() = RippleTheme.defaultRippleColor(
        contentColor = FinancialConnectionsTheme.colors.textBrand,
        lightTheme = MaterialTheme.colors.isLight,
    )

    @Composable
    override fun rippleAlpha() = RippleTheme.defaultRippleAlpha(
        contentColor = FinancialConnectionsTheme.colors.textBrand,
        lightTheme = MaterialTheme.colors.isLight,
    )
}

@Composable
internal fun FinancialConnectionsTheme(
    theme: Theme = Theme.default,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalNavHostController provides rememberNavController(),
        LocalTypography provides Typography,
        LocalColors provides theme.colors,
    ) {
        val view = LocalView.current
        val window = findWindow()
        val barColor = FinancialConnectionsTheme.colors.border
        if (!view.isInEditMode) {
            SideEffect {
                window?.let { window ->
                    val insets = WindowCompat.getInsetsController(window, view)
                    window.navigationBarColor = barColor.toArgb()
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
