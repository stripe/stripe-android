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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaymentsColors(
    val primary: Color,
    val surface: Color,
    val component: Color,
    val componentBorder: Color,
    val componentDivider: Color,
    val onComponent: Color,
    val subtitle: Color,
    val textCursor: Color,
    val placeholderText: Color,
    val onSurface: Color,
    val appBarIcon: Color,
    val error: Color,
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object PaymentsThemeConfig {
    fun colors(isDark: Boolean): PaymentsColors {
        return if (isDark) colorsDark else colorsLight
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object Shapes {
        val cornerRadius = 6.dp
        val borderStrokeWidth = 1.dp
        val borderStrokeWidthSelected = 2.dp
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object Typography {
        private val fontWeightBold: Int = FontWeight.Bold.weight
        private val fontWeightMedium: Int = FontWeight.Medium.weight
        private val fontWeightNormal: Int = FontWeight.Normal.weight
        private val fontSizeMultiplier: Float = 1.0F
        val fontFamily: Int = R.font.roboto

        // h4 is our largest headline. It is used for the most important labels in our UI
        // ex: "Select your payment method" in Payment Sheet.
        val h4 = TextStyle.Default.copy(
            fontFamily = FontFamily(Font(fontFamily)),
            fontSize = (20.0 * fontSizeMultiplier).sp,
            fontWeight = FontWeight(fontWeightBold),
        )

        // h5 is our medium headline label.
        // ex: "Pay $50.99" in Payment Sheet's buy button.
        val h5 = TextStyle.Default.copy(
            fontFamily = FontFamily(Font(fontFamily)),
            fontSize = (16.0 * fontSizeMultiplier).sp,
            fontWeight = FontWeight(fontWeightMedium),
            letterSpacing = (-0.32).sp
        )

        // h6 is our smallest headline label.
        // ex: Section labels in Payment Sheet
        val h6 = TextStyle.Default.copy(
            fontFamily = FontFamily(Font(fontFamily)),
            fontSize = (13.0 * fontSizeMultiplier).sp,
            fontWeight = FontWeight(fontWeightMedium),
            letterSpacing = (-0.15).sp
        )

        // body1 is our larger body text. Used for the bulk of our elements and forms.
        // ex: the text used in Payment Sheet's text form elements.
        val body1 = TextStyle.Default.copy(
            fontFamily = FontFamily(Font(fontFamily)),
            fontSize = (14.0 * fontSizeMultiplier).sp,
            fontWeight = FontWeight(fontWeightNormal),
        )

        // subtitle1 is our only subtitle size. Used for labeling fields.
        // ex: the placeholder texts that appear when you type in Payment Sheet's forms.
        val subtitle1 = TextStyle.Default.copy(
            fontFamily = FontFamily(Font(fontFamily)),
            fontSize = (14.0 * fontSizeMultiplier).sp,
            fontWeight = FontWeight(fontWeightNormal),
            letterSpacing = (-0.15).sp
        )

        // caption is used to label images in payment sheet.
        // ex: the labels under our payment method selectors in Payment Sheet.
        val caption = TextStyle.Default.copy(
            fontFamily = FontFamily(Font(fontFamily)),
            fontSize = (12.0 * fontSizeMultiplier).sp,
            fontWeight = FontWeight(fontWeightMedium)
        )

        // body2 is our smaller body text. Used for less important fields that are not required to
        // read. Ex: our mandate texts in Payment Sheet.
        val body2 = TextStyle.Default.copy(
            fontFamily = FontFamily(Font(fontFamily)),
            fontSize = (9.0 * fontSizeMultiplier).sp,
            fontWeight = FontWeight(fontWeightNormal),
            letterSpacing = (-0.15).sp
        )
    }

    private val colorsLight = PaymentsColors(
        primary = Color(0xFF007AFF),
        surface = Color.White,
        component = Color.White,
        componentBorder = Color(0x33787880),
        componentDivider = Color(0x33787880),
        onComponent = Color.Black,
        subtitle = Color(0x99000000),
        textCursor = Color.Black,
        placeholderText = Color(0x993C3C43),
        onSurface = Color.Black,
        appBarIcon = Color(0x99000000),
        error = Color.Red,
    )

    private val colorsDark = PaymentsColors(
        primary = Color(0xFF0074D4),
        surface = Color(0xff2e2e2e),
        component = Color.DarkGray,
        componentBorder = Color(0xFF787880),
        componentDivider = Color(0xFF787880),
        onComponent = Color.White,
        subtitle = Color(0x99FFFFFF),
        textCursor = Color.White,
        placeholderText = Color(0x61FFFFFF),
        onSurface = Color.White,
        appBarIcon = Color.White,
        error = Color.Red,
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaymentsComposeColors(
    val component: Color,
    val colorComponentBorder: Color,
    val colorComponentDivider: Color,
    val subtitle: Color,
    val colorTextCursor: Color,
    val placeholderText: Color,
    val onComponent: Color,
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
fun PaymentsThemeConfig.toComposeColors(): PaymentsComposeColors {
    val colors = colors(isSystemInDarkTheme())
    return PaymentsComposeColors(
        component = colors.component,
        colorComponentBorder = colors.componentBorder,
        colorComponentDivider = colors.componentDivider,
        onComponent = colors.onComponent,
        subtitle = colors.subtitle,
        colorTextCursor = colors.textCursor,
        placeholderText = colors.placeholderText,

        material = lightColors(
            primary = colors.primary,
            surface = colors.surface,
            onSurface = colors.onSurface,
            error = colors.error,
        )
    )
}

@Composable
@ReadOnlyComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun PaymentsThemeConfig.Shapes.toComposeShapes(): PaymentsComposeShapes {
    return PaymentsComposeShapes(
        borderStrokeWidth = borderStrokeWidth,
        borderStrokeWidthSelected = borderStrokeWidthSelected,
        material = MaterialTheme.shapes.copy(
            small = RoundedCornerShape(cornerRadius),
            medium = RoundedCornerShape(cornerRadius)
        )
    )
}

@Composable
@ReadOnlyComposable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun PaymentsThemeConfig.Typography.toComposeTypography(): Typography {
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
    val colors = PaymentsThemeConfig.toComposeColors()
    val localColors = staticCompositionLocalOf { colors }

    val shapes = PaymentsThemeConfig.Shapes.toComposeShapes()
    val localShapes = staticCompositionLocalOf { shapes }

    CompositionLocalProvider(
        localColors provides colors,
        localShapes provides shapes
    ) {
        MaterialTheme(
            colors = PaymentsTheme.colors.material,
            typography = PaymentsThemeConfig.Typography.toComposeTypography(),
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
    const val minContrastForWhite = 2.2

    val colors: PaymentsComposeColors
        @Composable
        @ReadOnlyComposable
        get() = PaymentsThemeConfig.toComposeColors()

    val shapes: PaymentsComposeShapes
        @Composable
        @ReadOnlyComposable
        get() = PaymentsThemeConfig.Shapes.toComposeShapes()

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = PaymentsThemeConfig.Typography.toComposeTypography()

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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun createTextSpanFromTextStyle(
    text: String?,
    context: Context,
    textStyle: TextStyle,
    color: Color,
    @FontRes
    fontFamily: Int
): SpannableString {
    val span = SpannableString(text ?: "")

    val fontSize = context.convertDpToPx(textStyle.fontSize.value.dp)
    span.setSpan(AbsoluteSizeSpan(fontSize.toInt()), 0, span.length, 0)

    span.setSpan(ForegroundColorSpan(color.toArgb()), 0, span.length, 0)

    ResourcesCompat.getFont(
        context,
        fontFamily
    )?.let {
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
