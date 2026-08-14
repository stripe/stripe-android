package com.stripe.android.financialconnections.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.Colors
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.RippleDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.stripe.android.model.LinkBrand

internal enum class Theme {
    DefaultLight,
    LinkLight,

    /**
     * The Link DS 3.0 refresh of [LinkLight]. Only reachable when
     * [com.stripe.android.core.utils.FeatureFlags.financialConnectionsLinkDs3] is enabled — see
     * `FinancialConnectionsSheetNativeActivity.toLocalTheme`.
     */
    LinkDs3;

    val colors: FinancialConnectionsColors
        @Composable
        get() = when (this) {
            DefaultLight -> if (isSystemInDarkTheme()) DarkThemeColors else Colors
            LinkLight -> if (isSystemInDarkTheme()) InstantDebitsDarkModeColors else InstantDebitsColors
            LinkDs3 -> if (isSystemInDarkTheme()) LinkDs3DarkColors else LinkDs3Colors
        }

    fun icon(linkBrand: LinkBrand): Int = when (this) {
        DefaultLight -> R.drawable.stripe_logo
        LinkLight, LinkDs3 -> when (linkBrand) {
            LinkBrand.Onelink -> R.drawable.stripe_onelink_logo_monochrome
            LinkBrand.Link -> R.drawable.stripe_link_logo_monochrome
        }
    }

    companion object {
        val default: Theme = DefaultLight
    }
}

/**
 * Whether the Link DS 3.0 design is active. Prefer expressing differences as color tokens on
 * [FinancialConnectionsColors]; only branch on this for structural changes that no token can express
 * (pill buttons, grouped card lists, the warmup sheet's horizontal footer).
 */
internal val Theme.isLinkDs3: Boolean
    get() = this == Theme.LinkDs3

private val Colors = FinancialConnectionsColors(
    background = Neutral0,
    backgroundSecondary = Neutral25,
    backgroundHighlighted = Neutral50,
    textDefault = Neutral800,
    textSubdued = Neutral600,
    textCritical = FeedbackCritical600,
    icon = Neutral700,
    borderNeutral = Neutral100,
    spinnerNeutral = Neutral200,
    warningLight = Attention50,
    warning = Attention300,
    primary = Brand500,
    primaryAccent = Neutral0,
    textAction = Brand600,
    textFieldFocused = Brand600,
    logo = Brand600,
    iconTint = Brand500,
    iconBackground = Brand25,
    iconBackgroundOnCard = Brand25,
    spinner = Brand500,
    border = Brand600,
    successIconBackground = Brand500,
    successIconForeground = Neutral0,
)

private val DarkThemeColors = FinancialConnectionsColors(
    background = Neutral0Dark,
    backgroundSecondary = Neutral25Dark,
    backgroundHighlighted = Neutral50Dark,
    textDefault = Neutral25,
    textSubdued = Neutral800Dark,
    textCritical = FeedbackCritical600,
    icon = Neutral25,
    borderNeutral = Neutral100Dark,
    spinnerNeutral = Neutral200,
    warningLight = Attention100Dark,
    warning = Attention300,
    primary = Brand500,
    primaryAccent = Neutral0,
    textAction = Brand500,
    textFieldFocused = Brand600,
    logo = Neutral0,
    iconTint = Brand500,
    iconBackground = Brand25Dark,
    iconBackgroundOnCard = Brand25Dark,
    spinner = Brand500,
    border = Brand600,
    successIconBackground = Brand500,
    successIconForeground = Neutral0,
)

private val InstantDebitsColors = FinancialConnectionsColors(
    background = Neutral0,
    backgroundSecondary = Neutral25,
    backgroundHighlighted = Neutral50,
    textDefault = Neutral800,
    textSubdued = Neutral600,
    textCritical = FeedbackCritical600,
    icon = Neutral700,
    borderNeutral = Neutral100,
    spinnerNeutral = Neutral200,
    warningLight = Attention50,
    warning = Attention300,
    primary = LinkGreen200,
    primaryAccent = LinkGreen900,
    textAction = LinkGreen500,
    textFieldFocused = LinkGreen200,
    logo = LinkGreen900,
    iconTint = LinkGreen500,
    iconBackground = LinkGreen50,
    iconBackgroundOnCard = LinkGreen50,
    spinner = LinkGreen200,
    border = LinkGreen200,
    successIconBackground = LinkGreen200,
    successIconForeground = LinkGreen900,
)

private val InstantDebitsDarkModeColors = FinancialConnectionsColors(
    background = Neutral0Dark,
    backgroundSecondary = Neutral25Dark,
    backgroundHighlighted = Neutral50Dark,
    textDefault = Neutral25,
    textSubdued = Neutral800Dark,
    textCritical = FeedbackCritical600,
    icon = Neutral25,
    borderNeutral = Neutral100Dark,
    spinnerNeutral = Neutral200,
    warningLight = Attention100Dark,
    warning = Attention300,
    primary = LinkGreen200,
    primaryAccent = LinkGreen900,
    textAction = LinkGreen200,
    textFieldFocused = Brand600,
    logo = Neutral0,
    iconTint = LinkGreen500,
    iconBackground = LinkGreen50Dark,
    iconBackgroundOnCard = LinkGreen50Dark,
    spinner = LinkGreen200,
    border = LinkGreen200,
    successIconBackground = LinkGreen200,
    successIconForeground = LinkGreen900,
)

/**
 * Link DS 3.0, light. Neutral tokens are shared with [InstantDebitsColors]; the brand-driven tokens
 * move from green to the pure grey ramp, so the primary CTA becomes near-black rather than green.
 */
private val LinkDs3Colors = FinancialConnectionsColors(
    background = Neutral0,
    backgroundSecondary = Neutral25,
    backgroundHighlighted = Neutral50,
    textDefault = Neutral800,
    textSubdued = Neutral600,
    textCritical = FeedbackCritical600,
    icon = Neutral700,
    borderNeutral = Neutral100,
    spinnerNeutral = Neutral200,
    warningLight = Attention50,
    warning = Attention300,
    primary = LinkNeutral900,
    primaryAccent = LinkNeutral0,
    textAction = LinkBrand600,
    textFieldFocused = LinkNeutral900,
    logo = LinkNeutral900,
    iconTint = LinkNeutral900,
    iconBackground = LinkNeutral100,
    iconBackgroundOnCard = LinkNeutral200,
    spinner = LinkNeutral900,
    border = LinkNeutral900,
    successIconBackground = LinkGreen200,
    successIconForeground = LinkNeutral900,
)

/**
 * Link DS 3.0, dark. Note [primary] / [primaryAccent] invert relative to
 * [LinkDs3Colors], so the primary CTA is dark-on-white here.
 */
private val LinkDs3DarkColors = FinancialConnectionsColors(
    background = Neutral0Dark,
    backgroundSecondary = Neutral25Dark,
    backgroundHighlighted = Neutral50Dark,
    textDefault = Neutral25,
    textSubdued = Neutral800Dark,
    textCritical = FeedbackCritical600,
    icon = Neutral25,
    borderNeutral = Neutral100Dark,
    spinnerNeutral = Neutral200,
    warningLight = Attention100Dark,
    warning = Attention300,
    primary = LinkNeutral0,
    primaryAccent = LinkNeutral900,
    textAction = LinkGreen200,
    textFieldFocused = LinkNeutral0,
    logo = Neutral0,
    iconTint = LinkNeutral0,
    iconBackground = LinkNeutral800,
    iconBackgroundOnCard = LinkNeutral700,
    spinner = LinkNeutral0,
    border = LinkNeutral0,
    successIconBackground = LinkGreen200,
    successIconForeground = LinkNeutral900,
)

private val lineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

private val Typography = FinancialConnectionsTypography(
    headingXLarge = TextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.38.sp,
        fontWeight = FontWeight.W700,
        lineHeightStyle = lineHeightStyle
    ).toCompat(),
    headingXLargeSubdued = TextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
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

internal val TextSelectionColors: TextSelectionColors
    @Composable
    get() = TextSelectionColors(
        handleColor = FinancialConnectionsTheme.colors.textDefault,
        backgroundColor = FinancialConnectionsTheme.colors.textDefault.copy(alpha = 0.4f)
    )

private val FinancialConnectionsRippleConfiguration: RippleConfiguration
    @Composable
    get() {
        // DS 3.0's textAction is green, which tints every pressed row green. Press feedback there is
        // meant to be neutral, so it follows the text color instead.
        val rippleContentColor = if (FinancialConnectionsTheme.theme.isLinkDs3) {
            FinancialConnectionsTheme.colors.textDefault
        } else {
            FinancialConnectionsTheme.colors.textAction
        }
        return RippleConfiguration(
            color = RippleDefaults.rippleColor(
                contentColor = rippleContentColor,
                lightTheme = MaterialTheme.colors.isLight,
            ),
            rippleAlpha = RippleDefaults.rippleAlpha(
                contentColor = rippleContentColor,
                lightTheme = MaterialTheme.colors.isLight,
            )
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
        LocalTheme provides theme,
    ) {
        val view = LocalView.current
        val window = findWindow()
        val barColor = FinancialConnectionsTheme.colors.borderNeutral
        if (!view.isInEditMode) {
            val lightNavBar = !isSystemInDarkTheme()
            SideEffect {
                window?.let { window ->
                    val insets = WindowCompat.getInsetsController(window, view)
                    window.navigationBarColor = barColor.toArgb()
                    insets.isAppearanceLightNavigationBars = lightNavBar
                }
            }
        }

        MaterialTheme(
            colors = debugColors(),
            content = {
                CompositionLocalProvider(
                    LocalTextSelectionColors provides TextSelectionColors,
                    LocalTextStyle provides LocalTextStyle.current.toCompat(useDefaultLineHeight = true),
                    LocalRippleConfiguration provides FinancialConnectionsRippleConfiguration
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

private val LocalTheme =
    staticCompositionLocalOf<Theme> {
        error("no Theme provided")
    }

internal object FinancialConnectionsTheme {
    val typography
        @Composable
        get() = LocalTypography.current
    val colors
        @Composable
        get() = LocalColors.current
    val theme
        @Composable
        get() = LocalTheme.current
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
