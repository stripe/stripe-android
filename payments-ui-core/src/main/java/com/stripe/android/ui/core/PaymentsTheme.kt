package com.stripe.android.ui.core

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.ui.graphics.toArgb
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaymentsColors(
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
    val fontFamily: Int?
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
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PrimaryButtonShape(
    val cornerRadius: Float,
    val borderStrokeWidth: Float,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PrimaryButtonTypography(
    @FontRes
    val fontFamily: Int?,
    val fontSize: TextUnit
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object PaymentsThemeDefaults {
    fun colors(isDark: Boolean): PaymentsColors {
        return if (isDark) colorsDark else colorsLight
    }

    val colorsLight = PaymentsColors(
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

    val colorsDark = PaymentsColors(
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
            surface = Color(0xff2e2e2e),
            onSurface = Color.White,
            error = Color.Red
        )
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
        fontFamily = null // We default to the default system font.
    )

    val primaryButtonStyle = PrimaryButtonStyle(
        colorsLight = PrimaryButtonColors(
            background = colors(false).materialColors.primary,
            onBackground = Color.White,
            border = Color.Transparent
        ),
        colorsDark = PrimaryButtonColors(
            background = colors(true).materialColors.primary,
            onBackground = Color.White,
            border = Color.Transparent
        ),
        shape = PrimaryButtonShape(
            cornerRadius = shapes.cornerRadius,
            borderStrokeWidth = 0.0f,
        ),
        typography = PrimaryButtonTypography(
            fontFamily = typography.fontFamily,
            fontSize = typography.largeFontSize
        )
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaymentsComposeShapes(
    val borderStrokeWidth: Dp,
    val borderStrokeWidthSelected: Dp,
    val material: Shapes
)

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

    val fontFamily = if (fontFamily != null) FontFamily(Font(fontFamily)) else FontFamily.Default
    // h4 is our largest headline. It is used for the most important labels in our UI
    // ex: "Select your payment method" in Payment Sheet.
    val h4 = TextStyle.Default.copy(
        fontFamily = fontFamily,
        fontSize = (xLargeFontSize * fontSizeMultiplier),
        fontWeight = FontWeight(fontWeightBold),
    )

    // h5 is our medium headline label.
    // ex: "Pay $50.99" in Payment Sheet's buy button.
    val h5 = TextStyle.Default.copy(
        fontFamily = fontFamily,
        fontSize = (largeFontSize * fontSizeMultiplier),
        fontWeight = FontWeight(fontWeightMedium),
        letterSpacing = (-0.32).sp
    )

    // h6 is our smallest headline label.
    // ex: Section labels in Payment Sheet
    val h6 = TextStyle.Default.copy(
        fontFamily = fontFamily,
        fontSize = (smallFontSize * fontSizeMultiplier),
        fontWeight = FontWeight(fontWeightMedium),
        letterSpacing = (-0.15).sp
    )

    // body1 is our larger body text. Used for the bulk of our elements and forms.
    // ex: the text used in Payment Sheet's text form elements.
    val body1 = TextStyle.Default.copy(
        fontFamily = fontFamily,
        fontSize = (mediumFontSize * fontSizeMultiplier),
        fontWeight = FontWeight(fontWeightNormal),
    )

    // subtitle1 is our only subtitle size. Used for labeling fields.
    // ex: the placeholder texts that appear when you type in Payment Sheet's forms.
    val subtitle1 = TextStyle.Default.copy(
        fontFamily = fontFamily,
        fontSize = (mediumFontSize * fontSizeMultiplier),
        fontWeight = FontWeight(fontWeightNormal),
        letterSpacing = (-0.15).sp
    )

    // caption is used to label images in payment sheet.
    // ex: the labels under our payment method selectors in Payment Sheet.
    val caption = TextStyle.Default.copy(
        fontFamily = fontFamily,
        fontSize = (xSmallFontSize * fontSizeMultiplier),
        fontWeight = FontWeight(fontWeightMedium)
    )

    // body2 is our smaller body text. Used for less important fields that are not required to
    // read. Ex: our mandate texts in Payment Sheet.
    val body2 = TextStyle.Default.copy(
        fontFamily = fontFamily,
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

val LocalColors = staticCompositionLocalOf { PaymentsTheme.getColors(false) }
val LocalShapes = staticCompositionLocalOf { PaymentsTheme.shapesMutable }
val LocalTypography = staticCompositionLocalOf { PaymentsTheme.typographyMutable }

/**
 * Base Theme for Payments Composables.
 * CAUTION: This theme is mutable by merchant configurations. You shouldn't be passing colors,
 * shapes, typography to this theme outside of tests.
 */
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun PaymentsTheme(
    colors: PaymentsColors = PaymentsTheme.getColors(isSystemInDarkTheme()),
    shapes: PaymentsShapes = PaymentsTheme.shapesMutable,
    typography: PaymentsTypography = PaymentsTheme.typographyMutable,
    content: @Composable () -> Unit
) {
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

/**
 * Base Theme for Payments Composables that are not merchant configurable.
 * Use this theme if you do not want merchant configurations to change your UI
 */
@Composable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun DefaultPaymentsTheme(
    content: @Composable () -> Unit
) {
    val colors = PaymentsThemeDefaults.colors(isSystemInDarkTheme())
    val shapes = PaymentsThemeDefaults.shapes
    val typography = PaymentsThemeDefaults.typography

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

val MaterialTheme.paymentsColors: PaymentsColors
    @Composable
    @ReadOnlyComposable
    get() = LocalColors.current

val MaterialTheme.paymentsShapes: PaymentsShapes
    @Composable
    @ReadOnlyComposable
    get() = LocalShapes.current

@Composable
@ReadOnlyComposable
fun MaterialTheme.getBorderStroke(isSelected: Boolean): BorderStroke {
    val width = if (isSelected) paymentsShapes.borderStrokeWidthSelected else paymentsShapes.borderStrokeWidth
    val color = if (isSelected) paymentsColors.materialColors.primary else paymentsColors.componentBorder
    return BorderStroke(width.dp, color)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object PaymentsTheme {
    const val minContrastForWhite = 2.2
    var colorsDarkMutable = PaymentsThemeDefaults.colorsDark
    var colorsLightMutable = PaymentsThemeDefaults.colorsLight

    var shapesMutable = PaymentsThemeDefaults.shapes

    var typographyMutable = PaymentsThemeDefaults.typography

    var primaryButtonStyle = PaymentsThemeDefaults.primaryButtonStyle

    fun getColors(isDark: Boolean): PaymentsColors {
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
    return if (contrastRatioToWhite > PaymentsTheme.minContrastForWhite) {
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
fun PrimaryButtonStyle.getOnBackgroundColor(context: Context): Int {
    val isDark = context.isSystemDarkTheme()
    return (if (isDark) colorsDark else colorsLight).onBackground.toArgb()
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
