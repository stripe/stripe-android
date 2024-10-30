package com.stripe.android.uicore

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
import androidx.annotation.ColorInt
import androidx.annotation.FontRes
import androidx.annotation.RestrictTo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import java.lang.Float.max

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class StripeColors(
    val component: Color,
    val componentBorder: Color,
    val componentDivider: Color,
    val onComponent: Color,
    val subtitle: Color,
    val textCursor: Color,
    val placeholderText: Color,
    val appBarIcon: Color,
    val materialColors: Colors
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class StripeShapes(
    val cornerRadius: Float,
    val borderStrokeWidth: Float,
) {

    val roundedCornerShape: Shape
        get() = RoundedCornerShape(size = cornerRadius.dp)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class StripeTypography(
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
    // global font overrides, takes precedence over individual font overrides below.
    @FontRes
    val fontFamily: Int?,
    // individual front overrides, only valid when fontFamily is null.
    val body1FontFamily: FontFamily? = null,
    val body2FontFamily: FontFamily? = null,
    val h4FontFamily: FontFamily? = null,
    val h5FontFamily: FontFamily? = null,
    val h6FontFamily: FontFamily? = null,
    val subtitle1FontFamily: FontFamily? = null,
    val captionFontFamily: FontFamily? = null
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PrimaryButtonStyle(
    val colorsLight: PrimaryButtonColors,
    val colorsDark: PrimaryButtonColors,
    val shape: PrimaryButtonShape,
    val typography: PrimaryButtonTypography
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PrimaryButtonColors(
    val background: Color,
    val onBackground: Color,
    val border: Color,
    val successBackground: Color,
    val onSuccessBackground: Color
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PrimaryButtonShape(
    val cornerRadius: Float,
    val borderStrokeWidth: Float
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PrimaryButtonTypography(
    @FontRes
    val fontFamily: Int?,
    val fontSize: TextUnit
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class EmbeddedFlatStyle(
    val separatorThickness: Float,
    val separatorColor: Color,
    val separatorInsets: Float,
    val topSeparatorEnabled: Boolean,
    val bottomSeparatorEnabled: Boolean
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class EmbeddedRadioStyle(
    val selectedColor: Color,
    val unselectedColor: Color
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class EmbeddedInsets(
    val checkmarkInsetDp: Float,
    val additionalInsetsDp: Float
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class EmbeddedFloatingStyle(
    val spacing: Float
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val PRIMARY_BUTTON_SUCCESS_BACKGROUND_COLOR = Color(0xFF24B47E)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object StripeThemeDefaults {
    fun colors(isDark: Boolean): StripeColors {
        return if (isDark) colorsDark else colorsLight
    }

    val colorsLight = StripeColors(
        component = Color.White,
        componentBorder = Color(0x33787880),
        componentDivider = Color(0x33787880),
        onComponent = Color.Black,
        subtitle = Color(0x99000000),
        textCursor = Color.Black,
        placeholderText = Color(0x993C3C43),
        appBarIcon = Color(0x99000000),
        materialColors = lightColors(
            primary = Color(0xFF007AFF),
            surface = Color.White,
            onSurface = Color.Black,
            error = Color.Red
        )
    )

    val colorsDark = StripeColors(
        component = Color.DarkGray,
        componentBorder = Color(0xFF787880),
        componentDivider = Color(0xFF787880),
        onComponent = Color.White,
        subtitle = Color(0x99FFFFFF),
        textCursor = Color.White,
        placeholderText = Color(0x61FFFFFF),
        appBarIcon = Color.White,
        materialColors = darkColors(
            primary = Color(0xFF0074D4),
            surface = Color(0xFF2E2E2E),
            onSurface = Color.White,
            error = Color.Red
        )
    )

    val shapes = StripeShapes(
        cornerRadius = 6.0f,
        borderStrokeWidth = 1.0f,
    )

    val typography = StripeTypography(
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
        fontFamily = null // We default to the default system font.
    )

    val primaryButtonStyle = PrimaryButtonStyle(
        colorsLight = PrimaryButtonColors(
            background = colors(false).materialColors.primary,
            onBackground = Color.White,
            border = Color.Transparent,
            successBackground = PRIMARY_BUTTON_SUCCESS_BACKGROUND_COLOR,
            onSuccessBackground = Color.White
        ),
        colorsDark = PrimaryButtonColors(
            background = colors(true).materialColors.primary,
            onBackground = Color.White,
            border = Color.Transparent,
            successBackground = PRIMARY_BUTTON_SUCCESS_BACKGROUND_COLOR,
            onSuccessBackground = Color.White
        ),
        shape = PrimaryButtonShape(
            cornerRadius = shapes.cornerRadius,
            borderStrokeWidth = 0.0f
        ),
        typography = PrimaryButtonTypography(
            fontFamily = typography.fontFamily,
            fontSize = typography.largeFontSize
        )
    )

    val flat = EmbeddedFlatStyle(
        separatorThickness = 1.0f,
        separatorColor = Color(0xFF787880),
        separatorInsets = 0.0f,
        topSeparatorEnabled = true,
        bottomSeparatorEnabled = true
    )

    val embeddedCommon = EmbeddedInsets(
        additionalInsetsDp = 4.0f,
        checkmarkInsetDp = 12.0f
    )

    val floating = EmbeddedFloatingStyle(
        spacing = 12.0f
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class StripeComposeShapes(
    val borderStrokeWidth: Dp,
    val material: Shapes
)

@Composable
@ReadOnlyComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun StripeShapes.toComposeShapes(): StripeComposeShapes {
    return StripeComposeShapes(
        borderStrokeWidth = borderStrokeWidth.dp,
        material = MaterialTheme.shapes.copy(
            small = RoundedCornerShape(cornerRadius.dp),
            medium = RoundedCornerShape(cornerRadius.dp)
        )
    )
}

@Composable
@ReadOnlyComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun StripeTypography.toComposeTypography(): Typography {
    val globalFontFamily = fontFamily?.let { FontFamily(Font(it)) }
    val defaultTextStyle = TextStyle.Default.toCompat()

    // h4 is our largest headline. It is used for the most important labels in our UI
    // ex: "Select your payment method" in Payment Sheet.
    val h4 = defaultTextStyle.copy(
        fontFamily = globalFontFamily ?: h4FontFamily ?: FontFamily.Default,
        fontSize = (xLargeFontSize * fontSizeMultiplier),
        fontWeight = FontWeight(fontWeightBold)
    )

    // h5 is our medium headline label.
    // ex: "Pay $50.99" in Payment Sheet's buy button.
    val h5 = defaultTextStyle.copy(
        fontFamily = globalFontFamily ?: h5FontFamily ?: FontFamily.Default,
        fontSize = (largeFontSize * fontSizeMultiplier),
        fontWeight = FontWeight(fontWeightMedium),
        letterSpacing = (-0.32).sp
    )

    // h6 is our smallest headline label.
    // ex: Section labels in Payment Sheet
    val h6 = defaultTextStyle.copy(
        fontFamily = globalFontFamily ?: h6FontFamily ?: FontFamily.Default,
        fontSize = (smallFontSize * fontSizeMultiplier),
        fontWeight = FontWeight(fontWeightMedium),
        letterSpacing = (-0.15).sp
    )

    // body1 is our larger body text. Used for the bulk of our elements and forms.
    // ex: the text used in Payment Sheet's text form elements.
    val body1 = defaultTextStyle.copy(
        fontFamily = globalFontFamily ?: body1FontFamily ?: FontFamily.Default,
        fontSize = (mediumFontSize * fontSizeMultiplier),
        fontWeight = FontWeight(fontWeightNormal)
    )

    // subtitle1 is our only subtitle size. Used for labeling fields.
    // ex: the placeholder texts that appear when you type in Payment Sheet's forms.
    val subtitle1 = defaultTextStyle.copy(
        fontFamily = globalFontFamily ?: subtitle1FontFamily ?: FontFamily.Default,
        fontSize = (mediumFontSize * fontSizeMultiplier),
        fontWeight = FontWeight(fontWeightNormal),
        letterSpacing = (-0.15).sp
    )

    // caption is used to label images in payment sheet.
    // ex: the labels under our payment method selectors in Payment Sheet.
    val caption = defaultTextStyle.copy(
        fontFamily = globalFontFamily ?: captionFontFamily ?: FontFamily.Default,
        fontSize = (xSmallFontSize * fontSizeMultiplier),
        fontWeight = FontWeight(fontWeightMedium)
    )

    // body2 is our smaller body text. Used for less important fields that are not required to
    // read. Ex: our mandate texts in Payment Sheet.
    val body2 = defaultTextStyle.copy(
        fontFamily = globalFontFamily ?: body2FontFamily ?: FontFamily.Default,
        fontSize = (xxSmallFontSize * fontSizeMultiplier),
        fontWeight = FontWeight(fontWeightNormal),
        letterSpacing = (-0.15).sp
    )

    val materialTypography = MaterialTheme.typography

    return materialTypography.copy(
        body1 = body1,
        body2 = body2,
        h1 = materialTypography.h1.toCompat(),
        h2 = materialTypography.h2.toCompat(),
        h3 = materialTypography.h3.toCompat(),
        h4 = h4,
        h5 = h5,
        h6 = h6,
        subtitle1 = subtitle1,
        subtitle2 = materialTypography.subtitle2.toCompat(),
        button = materialTypography.button.toCompat(),
        caption = caption,
        overline = materialTypography.overline.toCompat()
    )
}

val LocalColors = staticCompositionLocalOf { StripeTheme.getColors(false) }
val LocalShapes = staticCompositionLocalOf { StripeTheme.shapesMutable }
val LocalTypography = staticCompositionLocalOf { StripeTheme.typographyMutable }

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LocalInstrumentationTest = staticCompositionLocalOf { false }

/**
 * Base Theme for Stripe Composables.
 * CAUTION: This theme is mutable by merchant configurations. You shouldn't be passing colors,
 * shapes, typography to this theme outside of tests.
 */
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun StripeTheme(
    colors: StripeColors = StripeTheme.getColors(isSystemInDarkTheme()),
    shapes: StripeShapes = StripeTheme.shapesMutable,
    typography: StripeTypography = StripeTheme.typographyMutable,
    content: @Composable () -> Unit
) {
    val isRobolectricTest = runCatching {
        BuildConfig.DEBUG && Build.FINGERPRINT.lowercase() == "robolectric"
    }.getOrDefault(false)

    val isInstrumentationTest = runCatching {
        BuildConfig.DEBUG && run {
            // InstrumentationRegistry.getInstrumentation()
            val registry = Class.forName("androidx.test.platform.app.InstrumentationRegistry")
            val instrumentationMethod = registry.getDeclaredMethod("getInstrumentation")
            instrumentationMethod.invoke(null) // This will throw if we're not in an instrumentation test.
            true
        }
    }.getOrDefault(false)

    val inspectionMode = LocalInspectionMode.current || isRobolectricTest

    CompositionLocalProvider(
        LocalColors provides colors,
        LocalShapes provides shapes,
        LocalTypography provides typography,
        LocalInspectionMode provides inspectionMode,
        LocalInstrumentationTest provides isInstrumentationTest,
    ) {
        MaterialTheme(
            colors = colors.materialColors,
            typography = typography.toComposeTypography(),
            shapes = shapes.toComposeShapes().material,
        ) {
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.toCompat(),
            ) {
                content()
            }
        }
    }
}

/**
 * Base Theme for Payments Composables that are not merchant configurable.
 * Use this theme if you do not want merchant configurations to change your UI
 */
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun DefaultStripeTheme(
    content: @Composable () -> Unit
) {
    val colors = StripeThemeDefaults.colors(isSystemInDarkTheme())
    val shapes = StripeThemeDefaults.shapes
    val typography = StripeThemeDefaults.typography

    CompositionLocalProvider(
        LocalColors provides colors,
        LocalShapes provides shapes,
        LocalTypography provides typography
    ) {
        MaterialTheme(
            colors = colors.materialColors,
            typography = typography.toComposeTypography(),
            shapes = shapes.toComposeShapes().material,
            content = content
        )
    }
}

@Suppress("UnusedReceiverParameter")
val MaterialTheme.stripeColors: StripeColors
    @Composable
    @ReadOnlyComposable
    get() = LocalColors.current

@Suppress("UnusedReceiverParameter")
val MaterialTheme.stripeShapes: StripeShapes
    @Composable
    @ReadOnlyComposable
    get() = LocalShapes.current

@Suppress("UnusedReceiverParameter")
val MaterialTheme.stripeTypography: StripeTypography
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Composable
    @ReadOnlyComposable
    get() = LocalTypography.current

@Composable
@ReadOnlyComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun MaterialTheme.getBorderStrokeWidth(isSelected: Boolean) =
    if (isSelected) max(stripeShapes.borderStrokeWidth, 2f).dp else stripeShapes.borderStrokeWidth.dp

@Composable
@ReadOnlyComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun MaterialTheme.getBorderStrokeColor(isSelected: Boolean) =
    if (isSelected) stripeColors.materialColors.primary else stripeColors.componentBorder

@Composable
@ReadOnlyComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun MaterialTheme.getBorderStroke(isSelected: Boolean): BorderStroke =
    BorderStroke(getBorderStrokeWidth(isSelected), getBorderStrokeColor(isSelected))

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object StripeTheme {
    const val minContrastForWhite = 2.2
    var colorsDarkMutable = StripeThemeDefaults.colorsDark
    var colorsLightMutable = StripeThemeDefaults.colorsLight

    var shapesMutable = StripeThemeDefaults.shapes

    var typographyMutable = StripeThemeDefaults.typography

    var primaryButtonStyle = StripeThemeDefaults.primaryButtonStyle

    fun getColors(isDark: Boolean): StripeColors {
        return if (isDark) colorsDarkMutable else colorsLightMutable
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun createTextSpanFromTextStyle(
    text: String?,
    context: Context,
    fontSizeDp: Dp,
    color: Color,
    @FontRes
    fontFamily: Int?
): SpannableString {
    val span = SpannableString(text ?: "")

    val fontSize = context.convertDpToPx(fontSizeDp)
    span.setSpan(AbsoluteSizeSpan(fontSize.toInt()), 0, span.length, 0)

    span.setSpan(ForegroundColorSpan(color.toArgb()), 0, span.length, 0)

    if (fontFamily != null) {
        ResourcesCompat.getFont(
            context,
            fontFamily
        )
    } else {
        Typeface.DEFAULT
    }?.let {
        span.setSpan(CustomTypefaceSpan(it), 0, span.length, 0)
    }

    return span
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CustomTypefaceSpan(private val typeface: Typeface) : MetricAffectingSpan() {
    override fun updateDrawState(ds: TextPaint) {
        applyCustomTypeFace(ds, typeface)
    }

    override fun updateMeasureState(paint: TextPaint) {
        applyCustomTypeFace(paint, typeface)
    }

    companion object {
        private fun applyCustomTypeFace(paint: Paint, tf: Typeface) {
            paint.typeface = tf
        }
    }
}

// This method calculates if black or white offers a better contrast compared to a given color.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Color.shouldUseDarkDynamicColor(): Boolean {
    val contrastRatioToBlack = ColorUtils.calculateContrast(this.toArgb(), Color.Black.toArgb())
    val contrastRatioToWhite = ColorUtils.calculateContrast(this.toArgb(), Color.White.toArgb())

    // Prefer white as long as the min contrast has been met.
    return if (contrastRatioToWhite > StripeTheme.minContrastForWhite) {
        false
    } else {
        contrastRatioToBlack > contrastRatioToWhite
    }
}

@ColorInt
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun PrimaryButtonStyle.getBackgroundColor(context: Context): Int {
    val isDark = context.isSystemDarkTheme()
    return (if (isDark) colorsDark else colorsLight).background.toArgb()
}

@ColorInt
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun PrimaryButtonStyle.getSuccessBackgroundColor(context: Context): Int {
    val isDark = context.isSystemDarkTheme()
    return (if (isDark) colorsDark else colorsLight).successBackground.toArgb()
}

@ColorInt
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun PrimaryButtonStyle.getOnBackgroundColor(context: Context): Int {
    val isDark = context.isSystemDarkTheme()
    return (if (isDark) colorsDark else colorsLight).onBackground.toArgb()
}

@ColorInt
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun PrimaryButtonStyle.getOnSuccessBackgroundColor(context: Context): Int {
    val isDark = context.isSystemDarkTheme()
    return (if (isDark) colorsDark else colorsLight).onSuccessBackground.toArgb()
}

@ColorInt
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun PrimaryButtonStyle.getBorderStrokeColor(context: Context): Int {
    val isDark = context.isSystemDarkTheme()
    return (if (isDark) colorsDark else colorsLight).border.toArgb()
}

@Composable
@ReadOnlyComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun PrimaryButtonStyle.getComposeTextStyle(): TextStyle {
    val baseStyle = MaterialTheme.typography.h5.copy(
        color = (if (isSystemInDarkTheme()) colorsDark else colorsLight).onBackground,
        fontSize = typography.fontSize
    )
    return if (typography.fontFamily != null) {
        baseStyle.copy(fontFamily = FontFamily(Font(typography.fontFamily)))
    } else {
        baseStyle
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Context.getRawValueFromDimenResource(resource: Int): Float {
    return resources.getDimension(resource) / resources.displayMetrics.density
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Color.lighten(amount: Float): Color {
    return modifyBrightness {
        max(it + amount, 1f)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Color.darken(amount: Float): Color {
    return modifyBrightness {
        max(it - amount, 0f)
    }
}

private fun TextStyle.toCompat(): TextStyle {
    return copy(
        lineHeight = TextStyle.Default.lineHeight,
        lineHeightStyle = TextStyle.Default.lineHeightStyle,
        platformStyle = PlatformTextStyle(includeFontPadding = true),
    )
}

private fun Color.modifyBrightness(transform: (Float) -> Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this.toArgb(), hsl)
    val hue = hsl[0]
    val saturation = hsl[1]
    val lightness = hsl[2]
    return Color.hsl(hue, saturation, transform(lightness))
}
