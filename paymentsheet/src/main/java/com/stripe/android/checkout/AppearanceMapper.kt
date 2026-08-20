@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import com.stripe.android.elements.PaymentElement.Configuration.Appearance
import com.stripe.android.paymentsheet.PaymentSheet

internal fun Appearance.State.asPaymentSheet(): PaymentSheet.Appearance {
    return PaymentSheet.Appearance.Builder()
        .colorsLight(colorsLight.asPaymentSheet())
        .colorsDark(colorsDark.asPaymentSheet())
        .themeMode(themeMode.asPaymentSheet())
        .primaryButton(primaryButton.asPaymentSheet())
        .formInsetValues(formInsetValues.asPaymentSheet())
        .build()
}

private fun Appearance.Colors.State.asPaymentSheet(): PaymentSheet.Colors = PaymentSheet.Colors(
    primary = primary, surface = surface, component = component, componentBorder = componentBorder,
    componentDivider = componentDivider, onComponent = onComponent, onSurface = onSurface,
    subtitle = subtitle, placeholderText = placeholderText, appBarIcon = appBarIcon, error = error,
)

private fun Appearance.PrimaryButton.State.asPaymentSheet(): PaymentSheet.PrimaryButton = PaymentSheet.PrimaryButton(
    colorsLight = colorsLight.asPaymentSheet(), colorsDark = colorsDark.asPaymentSheet(),
    shape = PaymentSheet.PrimaryButtonShape(shape.cornerRadiusDp, shape.borderStrokeWidthDp, shape.heightDp),
    typography = PaymentSheet.PrimaryButtonTypography(typography.fontResId, typography.fontSizeSp),
)

private fun Appearance.PrimaryButton.Colors.State.asPaymentSheet(): PaymentSheet.PrimaryButtonColors =
    PaymentSheet.PrimaryButtonColors(
        background = background,
        onBackground = onBackground,
        border = border,
        successBackgroundColor = successBackgroundColor,
        onSuccessBackgroundColor = onSuccessBackgroundColor,
    )

private fun Appearance.Insets.State.asPaymentSheet(): PaymentSheet.Insets =
    PaymentSheet.Insets(startDp, topDp, endDp, bottomDp)

private fun Appearance.ThemeMode.asPaymentSheet(): PaymentSheet.ThemeMode = when (this) {
    Appearance.ThemeMode.Automatic -> PaymentSheet.ThemeMode.Automatic
    Appearance.ThemeMode.AlwaysLight -> PaymentSheet.ThemeMode.AlwaysLight
    Appearance.ThemeMode.AlwaysDark -> PaymentSheet.ThemeMode.AlwaysDark
}
