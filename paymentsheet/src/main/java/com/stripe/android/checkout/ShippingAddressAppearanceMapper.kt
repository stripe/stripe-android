package com.stripe.android.checkout

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
    return PaymentSheet.Colors(
        primary = primary?.let { Color(it) } ?: defaults.materialColors.primary,
        surface = surface?.let { Color(it) } ?: defaults.materialColors.surface,
        component = component?.let { Color(it) } ?: defaults.component,
        componentBorder = componentBorder?.let { Color(it) } ?: defaults.componentBorder,
        componentDivider = componentDivider?.let { Color(it) } ?: defaults.componentDivider,
        onComponent = onComponent?.let { Color(it) } ?: defaults.onComponent,
        onSurface = onSurface?.let { Color(it) } ?: defaults.materialColors.onSurface,
        subtitle = subtitle?.let { Color(it) } ?: defaults.subtitle,
        placeholderText = placeholderText?.let { Color(it) } ?: defaults.placeholderText,
        appBarIcon = appBarIcon?.let { Color(it) } ?: defaults.appBarIcon,
        error = error?.let { Color(it) } ?: defaults.materialColors.error,
    )
}

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
