package com.stripe.android.checkout

import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.StripeThemeDefaults

@OptIn(CheckoutSessionPreview::class)
internal fun ShippingAddressElement.Appearance.State.asPaymentSheet():
    PaymentSheet.Appearance = PaymentSheet.Appearance(
    colorsLight = colorsLight.asPaymentSheetColors(isLight = true),
    colorsDark = colorsDark.asPaymentSheetColors(isLight = false),
    shapes = shapes.asPaymentSheetShapes(),
    typography = typography.asPaymentSheetTypography(),
)

@Suppress("CyclomaticComplexMethod")
@OptIn(CheckoutSessionPreview::class)
private fun ShippingAddressElement.Appearance.Colors.State.asPaymentSheetColors(
    isLight: Boolean,
): PaymentSheet.Colors {
    val defaults = if (isLight) StripeThemeDefaults.colorsLight else StripeThemeDefaults.colorsDark
    val errorColor = colorOrDefault(this@asPaymentSheetColors.error, defaults.materialColors.error)
    return PaymentSheet.Colors(
        primary = colorOrDefault(primary, defaults.materialColors.primary),
        surface = colorOrDefault(surface, defaults.materialColors.surface),
        component = colorOrDefault(component, defaults.component),
        componentBorder = colorOrDefault(componentBorder, defaults.componentBorder),
        componentDivider = colorOrDefault(componentDivider, defaults.componentDivider),
        onComponent = colorOrDefault(onComponent, defaults.onComponent),
        onSurface = colorOrDefault(onSurface, defaults.materialColors.onSurface),
        subtitle = colorOrDefault(subtitle, defaults.subtitle),
        placeholderText = colorOrDefault(placeholderText, defaults.placeholderText),
        appBarIcon = colorOrDefault(appBarIcon, defaults.appBarIcon),
        error = errorColor,
    )
}

private fun colorOrDefault(@ColorInt value: Int?, default: Color): Color =
    if (value != null) Color(value) else default

@OptIn(CheckoutSessionPreview::class)
private fun ShippingAddressElement.Appearance.Shapes.State.asPaymentSheetShapes():
    PaymentSheet.Shapes = PaymentSheet.Shapes(
    cornerRadiusDp = cornerRadiusDp ?: StripeThemeDefaults.shapes.cornerRadius,
    borderStrokeWidthDp = borderStrokeWidthDp ?: StripeThemeDefaults.shapes.borderStrokeWidth,
)

@OptIn(CheckoutSessionPreview::class)
private fun ShippingAddressElement.Appearance.Typography.State.asPaymentSheetTypography():
    PaymentSheet.Typography = PaymentSheet.Typography.Builder()
    .sizeScaleFactor(sizeScaleFactor ?: StripeThemeDefaults.typography.fontSizeMultiplier)
    .fontResId(fontResId ?: StripeThemeDefaults.typography.fontFamily)
    .build()
