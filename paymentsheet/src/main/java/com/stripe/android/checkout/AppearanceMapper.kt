@file:OptIn(
    com.stripe.android.paymentelement.CheckoutSessionPreview::class,
    com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview::class,
)

package com.stripe.android.checkout

import com.stripe.android.elements.PaymentElement.Configuration.Appearance
import com.stripe.android.elements.ShippingAddressElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.Appearance.Embedded.RowStyle as PaymentSheetRowStyle

internal fun Appearance.State.asPaymentSheet(): PaymentSheet.Appearance {
    return PaymentSheet.Appearance.Builder()
        .colorsLight(colorsLight.asPaymentSheet())
        .colorsDark(colorsDark.asPaymentSheet())
        .themeMode(themeMode.asPaymentSheet())
        .shapes(shapes.asPaymentSheet())
        .typography(typography.asPaymentSheet())
        .embeddedAppearance(embeddedAppearance.asPaymentSheet())
        .primaryButton(primaryButton.asPaymentSheet())
        .formInsetValues(formInsetValues.asPaymentSheet())
        .build()
}

internal fun ShippingAddressElement.Configuration.Appearance.State.asPaymentSheet(): PaymentSheet.Appearance {
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

private fun ShippingAddressElement.Configuration.Appearance.Colors.State.asPaymentSheet(): PaymentSheet.Colors =
    PaymentSheet.Colors(
        primary = primary,
        surface = surface,
        component = component,
        componentBorder = componentBorder,
        componentDivider = componentDivider,
        onComponent = onComponent,
        onSurface = onSurface,
        subtitle = subtitle,
        placeholderText = placeholderText,
        appBarIcon = appBarIcon,
        error = error,
    )

private fun ShippingAddressElement.Configuration.Appearance.PrimaryButton.State.asPaymentSheet():
    PaymentSheet.PrimaryButton = PaymentSheet.PrimaryButton(
        colorsLight = colorsLight.asPaymentSheet(),
        colorsDark = colorsDark.asPaymentSheet(),
        shape = PaymentSheet.PrimaryButtonShape(
            shape.cornerRadiusDp,
            shape.borderStrokeWidthDp,
            shape.heightDp,
        ),
        typography = PaymentSheet.PrimaryButtonTypography(
            typography.fontResId,
            typography.fontSizeSp,
        ),
    )

private fun ShippingAddressElement.Configuration.Appearance.PrimaryButton.Colors.State.asPaymentSheet():
    PaymentSheet.PrimaryButtonColors = PaymentSheet.PrimaryButtonColors(
        background = background,
        onBackground = onBackground,
        border = border,
        successBackgroundColor = successBackgroundColor,
        onSuccessBackgroundColor = onSuccessBackgroundColor,
    )

private fun ShippingAddressElement.Configuration.Appearance.Insets.State.asPaymentSheet(): PaymentSheet.Insets =
    PaymentSheet.Insets(startDp, topDp, endDp, bottomDp)

private fun ShippingAddressElement.Configuration.Appearance.ThemeMode.asPaymentSheet(): PaymentSheet.ThemeMode =
    when (this) {
        ShippingAddressElement.Configuration.Appearance.ThemeMode.Automatic -> PaymentSheet.ThemeMode.Automatic
        ShippingAddressElement.Configuration.Appearance.ThemeMode.AlwaysLight -> PaymentSheet.ThemeMode.AlwaysLight
        ShippingAddressElement.Configuration.Appearance.ThemeMode.AlwaysDark -> PaymentSheet.ThemeMode.AlwaysDark
    }

private fun Appearance.Shapes.State.asPaymentSheet(): PaymentSheet.Shapes = PaymentSheet.Shapes(
    cornerRadiusDp = cornerRadiusDp,
    borderStrokeWidthDp = borderStrokeWidthDp,
    bottomSheetCornerRadiusDp = bottomSheetCornerRadiusDp,
)

private fun Appearance.Typography.State.asPaymentSheet(): PaymentSheet.Typography = PaymentSheet.Typography(
    sizeScaleFactor = sizeScaleFactor,
    fontResId = fontResId,
    custom = PaymentSheet.Typography.Custom(h1 = custom.h1?.asPaymentSheet()),
)

private fun Appearance.Typography.Font.State.asPaymentSheet(): PaymentSheet.Typography.Font =
    PaymentSheet.Typography.Font(
        fontFamily = fontFamily,
        fontSizeSp = fontSizeSp,
        fontWeight = fontWeight,
        letterSpacingSp = letterSpacingSp,
    )

private fun Appearance.Embedded.State.asPaymentSheet(): PaymentSheet.Appearance.Embedded =
    PaymentSheet.Appearance.Embedded(
        style = style.asPaymentSheet(),
        paymentMethodIconMargins = paymentMethodIconMargins?.asPaymentSheet(),
        titleFont = titleFont?.asPaymentSheet(),
        subtitleFont = subtitleFont?.asPaymentSheet(),
    )

private fun Appearance.Embedded.RowStyle.State.asPaymentSheet(): PaymentSheetRowStyle = when (this) {
    is Appearance.Embedded.RowStyle.FlatWithRadio.State -> asPaymentSheet()
    is Appearance.Embedded.RowStyle.FlatWithCheckmark.State -> asPaymentSheet()
    is Appearance.Embedded.RowStyle.FloatingButton.State -> asPaymentSheet()
    is Appearance.Embedded.RowStyle.FlatWithDisclosure.State -> asPaymentSheet()
}

private fun Appearance.Embedded.RowStyle.FlatWithRadio.State.asPaymentSheet(): PaymentSheetRowStyle.FlatWithRadio {
    return PaymentSheetRowStyle.FlatWithRadio(
        separatorThicknessDp = separatorThicknessDp,
        startSeparatorInsetDp = startSeparatorInsetDp,
        endSeparatorInsetDp = endSeparatorInsetDp,
        topSeparatorEnabled = topSeparatorEnabled,
        bottomSeparatorEnabled = bottomSeparatorEnabled,
        additionalVerticalInsetsDp = additionalVerticalInsetsDp,
        horizontalInsetsDp = horizontalInsetsDp,
        colorsLight = PaymentSheetRowStyle.FlatWithRadio.Colors(
            separatorColor = colorsLight.separatorColor,
            selectedColor = colorsLight.selectedColor,
            unselectedColor = colorsLight.unselectedColor,
        ),
        colorsDark = PaymentSheetRowStyle.FlatWithRadio.Colors(
            separatorColor = colorsDark.separatorColor,
            selectedColor = colorsDark.selectedColor,
            unselectedColor = colorsDark.unselectedColor,
        ),
    )
}

private fun Appearance.Embedded.RowStyle.FlatWithCheckmark.State.asPaymentSheet():
    PaymentSheetRowStyle.FlatWithCheckmark {
    return PaymentSheetRowStyle.FlatWithCheckmark(
        separatorThicknessDp = separatorThicknessDp,
        startSeparatorInsetDp = startSeparatorInsetDp,
        endSeparatorInsetDp = endSeparatorInsetDp,
        topSeparatorEnabled = topSeparatorEnabled,
        bottomSeparatorEnabled = bottomSeparatorEnabled,
        checkmarkInsetDp = checkmarkInsetDp,
        additionalVerticalInsetsDp = additionalVerticalInsetsDp,
        horizontalInsetsDp = horizontalInsetsDp,
        colorsLight = PaymentSheetRowStyle.FlatWithCheckmark.Colors(
            separatorColor = colorsLight.separatorColor,
            checkmarkColor = colorsLight.checkmarkColor,
        ),
        colorsDark = PaymentSheetRowStyle.FlatWithCheckmark.Colors(
            separatorColor = colorsDark.separatorColor,
            checkmarkColor = colorsDark.checkmarkColor,
        ),
    )
}

private fun Appearance.Embedded.RowStyle.FloatingButton.State.asPaymentSheet(): PaymentSheetRowStyle.FloatingButton {
    return PaymentSheetRowStyle.FloatingButton(
        spacingDp = spacingDp,
        additionalInsetsDp = additionalInsetsDp,
    )
}

private fun Appearance.Embedded.RowStyle.FlatWithDisclosure.State.asPaymentSheet():
    PaymentSheetRowStyle.FlatWithDisclosure {
    return PaymentSheetRowStyle.FlatWithDisclosure(
        separatorThicknessDp = separatorThicknessDp,
        startSeparatorInsetDp = startSeparatorInsetDp,
        endSeparatorInsetDp = endSeparatorInsetDp,
        topSeparatorEnabled = topSeparatorEnabled,
        additionalVerticalInsetsDp = additionalVerticalInsetsDp,
        bottomSeparatorEnabled = bottomSeparatorEnabled,
        horizontalInsetsDp = horizontalInsetsDp,
        colorsLight = PaymentSheetRowStyle.FlatWithDisclosure.Colors(
            separatorColor = colorsLight.separatorColor,
            disclosureColor = colorsLight.disclosureColor,
        ),
        colorsDark = PaymentSheetRowStyle.FlatWithDisclosure.Colors(
            separatorColor = colorsDark.separatorColor,
            disclosureColor = colorsDark.disclosureColor,
        ),
        disclosureIconRes = disclosureIconRes,
    )
}
