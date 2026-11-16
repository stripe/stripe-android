@file:OptIn(
    com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview::class,
    com.stripe.android.paymentelement.CheckoutSessionPreview::class,
)

package com.stripe.android.paymentsheet.example.playground.checkout.settings

import com.stripe.android.checkout.CheckoutElementsController
import com.stripe.android.elements.CurrencySelectorElement
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.elements.PaymentElement
import com.stripe.android.elements.ShippingAddressElement
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundDefinitions.Controller
import com.stripe.android.elements.PaymentElement.Configuration.Appearance.Embedded.RowStyle as EmbeddedRowStyle

internal fun CheckoutPlaygroundSettings.Snapshot.checkoutElementsControllerConfiguration():
    CheckoutElementsController.Configuration {
    return CheckoutElementsController.Configuration()
        .defaults(defaults())
        .apply {
            if (this@checkoutElementsControllerConfiguration[Controller.payment.shouldSetConfiguration]) {
                paymentElement(paymentElementConfiguration())
            }
            if (this@checkoutElementsControllerConfiguration[Controller.currencySelector.shouldSetConfiguration]) {
                currencySelectorElement(currencySelectorConfiguration())
            }
            if (this@checkoutElementsControllerConfiguration[Controller.shippingAddress.shouldSetConfiguration]) {
                shippingAddressElement(shippingAddressElementConfiguration())
            }
            if (this@checkoutElementsControllerConfiguration[Controller.express.shouldSetConfiguration]) {
                expressCheckoutElement(expressCheckoutConfiguration())
            }
            this@checkoutElementsControllerConfiguration[Controller.merchantDisplayName]?.let(::merchantDisplayName)
        }
}

private fun CheckoutPlaygroundSettings.Snapshot.defaults(): CheckoutElementsController.Configuration.Defaults {
    return CheckoutElementsController.Configuration.Defaults()
        .email(this[Controller.defaults.email])
        .apply {
            contactDetails(Controller.defaults.billing)?.let(::billingDetails)
            contactDetails(Controller.defaults.shipping)?.let(::shippingDetails)
        }
}

private fun CheckoutPlaygroundSettings.Snapshot.contactDetails(
    definitions: CheckoutContactDetailsDefinitions,
): CheckoutElementsController.Configuration.Defaults.ContactDetails? {
    if (!this[definitions.enabled]) return null
    return CheckoutElementsController.Configuration.Defaults.ContactDetails()
        .name(this[definitions.name])
        .apply {
            if (this@contactDetails[definitions.address.enabled]) {
                address(
                    CheckoutElementsController.Address(
                        country = this@contactDetails[definitions.address.country].uppercase(),
                    )
                        .city(this@contactDetails[definitions.address.city])
                        .line1(this@contactDetails[definitions.address.line1])
                        .line2(this@contactDetails[definitions.address.line2])
                        .postalCode(this@contactDetails[definitions.address.postalCode])
                        .state(this@contactDetails[definitions.address.state])
                )
            }
        }
}

private fun CheckoutPlaygroundSettings.Snapshot.paymentElementConfiguration(): PaymentElement.Configuration {
    return PaymentElement.Configuration()
        .embeddedViewDisplaysMandateText(this[Controller.payment.embeddedMandate])
        .paymentMethodLayout(this[Controller.payment.layout])
        .opensCardScannerAutomatically(this[Controller.payment.opensCardScanner])
        .preferredNetworks(this[Controller.payment.preferredNetworks])
        .paymentMethodOrder(this[Controller.payment.methodOrder])
        .cardBrandAcceptance(cardBrandAcceptance())
        .termsDisplay(termsDisplay())
        .appearance(paymentAppearance())
        .googlePayConfiguration(paymentGooglePayConfiguration())
        .linkConfiguration(
            PaymentElement.Configuration.LinkConfiguration()
                .display(this[Controller.payment.link.display])
        )
}

private fun CheckoutPlaygroundSettings.Snapshot.cardBrandAcceptance():
    PaymentElement.Configuration.CardBrandAcceptance {
    val brands = buildList {
        if (this@cardBrandAcceptance[Controller.payment.cardBrandAcceptance.visa]) {
            add(PaymentElement.Configuration.CardBrandAcceptance.BrandCategory.Visa)
        }
        if (this@cardBrandAcceptance[Controller.payment.cardBrandAcceptance.mastercard]) {
            add(PaymentElement.Configuration.CardBrandAcceptance.BrandCategory.Mastercard)
        }
        if (this@cardBrandAcceptance[Controller.payment.cardBrandAcceptance.amex]) {
            add(PaymentElement.Configuration.CardBrandAcceptance.BrandCategory.Amex)
        }
        if (this@cardBrandAcceptance[Controller.payment.cardBrandAcceptance.discover]) {
            add(PaymentElement.Configuration.CardBrandAcceptance.BrandCategory.Discover)
        }
    }
    return when (this[Controller.payment.cardBrandAcceptance.mode]) {
        CheckoutCardBrandAcceptanceMode.All -> PaymentElement.Configuration.CardBrandAcceptance.all()
        CheckoutCardBrandAcceptanceMode.Allowed -> PaymentElement.Configuration.CardBrandAcceptance.allowed(brands)
        CheckoutCardBrandAcceptanceMode.Disallowed -> {
            PaymentElement.Configuration.CardBrandAcceptance.disallowed(brands)
        }
    }
}

private fun CheckoutPlaygroundSettings.Snapshot.termsDisplay():
    Map<PaymentMethod.Type, PaymentElement.Configuration.TermsDisplay> {
    return Controller.payment.terms.values.mapNotNull { (type, definition) ->
        this[definition].configurationValue?.let { type to it }
    }.toMap()
}

private fun CheckoutPlaygroundSettings.Snapshot.paymentAppearance(): PaymentElement.Configuration.Appearance {
    val definitions = Controller.payment.appearance
    return PaymentElement.Configuration.Appearance()
        .colorsLight(paymentColors(definitions.colorsLight, light = true))
        .colorsDark(paymentColors(definitions.colorsDark, light = false))
        .themeMode(this[definitions.themeMode])
        .primaryButton(paymentPrimaryButton(definitions.primaryButton))
        .typography(paymentTypography(definitions.typography))
        .shapes(paymentShapes(definitions.shapes))
        .embeddedAppearance(embeddedAppearance(definitions.embeddedAppearance))
        .formInsetValues(
            PaymentElement.Configuration.Appearance.Insets(
                startDp = this[definitions.formInsetValues.startDp],
                topDp = this[definitions.formInsetValues.topDp],
                endDp = this[definitions.formInsetValues.endDp],
                bottomDp = this[definitions.formInsetValues.bottomDp],
            )
        )
}

private fun CheckoutPlaygroundSettings.Snapshot.paymentColors(
    definitions: CheckoutColorsDefinitions,
    light: Boolean,
): PaymentElement.Configuration.Appearance.Colors {
    val colors = if (light) {
        PaymentElement.Configuration.Appearance.Colors.light()
    } else {
        PaymentElement.Configuration.Appearance.Colors.dark()
    }
    return colors.apply {
        this@paymentColors[definitions.primary]?.let(::primary)
        this@paymentColors[definitions.surface]?.let(::surface)
        this@paymentColors[definitions.component]?.let(::component)
        this@paymentColors[definitions.componentBorder]?.let(::componentBorder)
        this@paymentColors[definitions.componentDivider]?.let(::componentDivider)
        this@paymentColors[definitions.onComponent]?.let(::onComponent)
        this@paymentColors[definitions.subtitle]?.let(::subtitle)
        this@paymentColors[definitions.placeholderText]?.let(::placeholderText)
        this@paymentColors[definitions.onSurface]?.let(::onSurface)
        this@paymentColors[definitions.appBarIcon]?.let(::appBarIcon)
        this@paymentColors[definitions.error]?.let(::error)
    }
}

private fun CheckoutPlaygroundSettings.Snapshot.paymentPrimaryButton(
    definitions: CheckoutPrimaryButtonDefinitions,
): PaymentElement.Configuration.Appearance.PrimaryButton {
    return PaymentElement.Configuration.Appearance.PrimaryButton()
        .colorsLight(paymentPrimaryButtonColors(definitions.colorsLight, light = true))
        .colorsDark(paymentPrimaryButtonColors(definitions.colorsDark, light = false))
        .shape(
            PaymentElement.Configuration.Appearance.PrimaryButton.Shape()
                .cornerRadiusDp(this[definitions.shape.cornerRadiusDp])
                .borderStrokeWidthDp(this[definitions.shape.borderStrokeWidthDp])
                .heightDp(this[definitions.shape.heightDp])
        )
        .typography(
            PaymentElement.Configuration.Appearance.PrimaryButton.Typography()
                .fontResId(this[definitions.typography.fontResId].resourceId)
                .fontSizeSp(this[definitions.typography.fontSizeSp])
        )
}

private fun CheckoutPlaygroundSettings.Snapshot.paymentPrimaryButtonColors(
    definitions: CheckoutPrimaryButtonDefinitions.ColorsDefinitions,
    light: Boolean,
): PaymentElement.Configuration.Appearance.PrimaryButton.Colors {
    val colors = if (light) {
        PaymentElement.Configuration.Appearance.PrimaryButton.Colors.light()
    } else {
        PaymentElement.Configuration.Appearance.PrimaryButton.Colors.dark()
    }
    return colors.apply {
        this@paymentPrimaryButtonColors[definitions.background]?.let(::background)
        this@paymentPrimaryButtonColors[definitions.onBackground]?.let(::onBackground)
        this@paymentPrimaryButtonColors[definitions.border]?.let(::border)
        this@paymentPrimaryButtonColors[definitions.successBackgroundColor]?.let(::successBackgroundColor)
        this@paymentPrimaryButtonColors[definitions.onSuccessBackgroundColor]?.let(::onSuccessBackgroundColor)
    }
}

private fun CheckoutPlaygroundSettings.Snapshot.paymentTypography(
    definitions: CheckoutTypographyDefinitions,
): PaymentElement.Configuration.Appearance.Typography {
    return PaymentElement.Configuration.Appearance.Typography()
        .sizeScaleFactor(this[definitions.sizeScaleFactor])
        .fontResId(this[definitions.fontResId].resourceId)
        .custom(
            PaymentElement.Configuration.Appearance.Typography.Custom()
                .h1(paymentFont(definitions.custom.h1))
        )
}

private fun CheckoutPlaygroundSettings.Snapshot.paymentFont(
    definitions: CheckoutFontDefinitions,
): PaymentElement.Configuration.Appearance.Typography.Font? {
    if (!definitions.isSet(this)) return null
    return PaymentElement.Configuration.Appearance.Typography.Font()
        .fontFamily(this[definitions.fontFamily].resourceId)
        .fontSizeSp(this[definitions.fontSizeSp])
        .fontWeight(this[definitions.fontWeight])
        .letterSpacingSp(this[definitions.letterSpacingSp])
}

private fun CheckoutPlaygroundSettings.Snapshot.paymentShapes(
    definitions: CheckoutShapesDefinitions,
): PaymentElement.Configuration.Appearance.Shapes {
    return PaymentElement.Configuration.Appearance.Shapes()
        .cornerRadiusDp(this[definitions.cornerRadiusDp])
        .borderStrokeWidthDp(this[definitions.borderStrokeWidthDp])
        .apply {
            this@paymentShapes[definitions.bottomSheetCornerRadiusDp]?.let(::bottomSheetCornerRadiusDp)
        }
}

private fun CheckoutPlaygroundSettings.Snapshot.embeddedAppearance(
    definitions: CheckoutEmbeddedAppearanceDefinitions,
): PaymentElement.Configuration.Appearance.Embedded {
    val rowStyle = definitions.rowStyle
    return PaymentElement.Configuration.Appearance.Embedded()
        .rowStyle(
            when (this[rowStyle.type]) {
                CheckoutEmbeddedRowStyle.FlatWithRadio -> flatWithRadio(rowStyle.flatWithRadio)
                CheckoutEmbeddedRowStyle.FlatWithCheckmark -> flatWithCheckmark(rowStyle.flatWithCheckmark)
                CheckoutEmbeddedRowStyle.FloatingButton -> floatingButton(rowStyle.floatingButton)
                CheckoutEmbeddedRowStyle.FlatWithDisclosure -> flatWithDisclosure(rowStyle.flatWithDisclosure)
            }
        )
        .paymentMethodIconMargins(paymentMethodIconMargins(definitions.paymentMethodIconMargins))
        .titleFont(paymentFont(definitions.titleFont))
        .subtitleFont(paymentFont(definitions.subtitleFont))
}

private fun CheckoutPlaygroundSettings.Snapshot.paymentMethodIconMargins(
    definitions: CheckoutOptionalInsetsDefinitions,
): PaymentElement.Configuration.Appearance.Insets? {
    if (!definitions.isSet(this)) return null
    return PaymentElement.Configuration.Appearance.Insets(
        startDp = this[definitions.startDp] ?: 0f,
        topDp = this[definitions.topDp] ?: 0f,
        endDp = this[definitions.endDp] ?: 0f,
        bottomDp = this[definitions.bottomDp] ?: 0f,
    )
}

private fun CheckoutPlaygroundSettings.Snapshot.flatWithRadio(
    definitions: CheckoutEmbeddedAppearanceDefinitions.FlatWithRadioDefinitions,
): EmbeddedRowStyle.FlatWithRadio {
    val flat = definitions.flat
    return EmbeddedRowStyle.FlatWithRadio()
        .separatorThicknessDp(this[flat.separatorThicknessDp])
        .startSeparatorInsetDp(this[flat.startSeparatorInsetDp])
        .endSeparatorInsetDp(this[flat.endSeparatorInsetDp])
        .topSeparatorEnabled(this[flat.topSeparatorEnabled])
        .bottomSeparatorEnabled(this[flat.bottomSeparatorEnabled])
        .additionalVerticalInsetsDp(this[flat.additionalVerticalInsetsDp])
        .horizontalInsetsDp(this[flat.horizontalInsetsDp])
        .colorsLight(radioColors(definitions.colorsLight, light = true))
        .colorsDark(radioColors(definitions.colorsDark, light = false))
}

private fun CheckoutPlaygroundSettings.Snapshot.radioColors(
    definitions: CheckoutEmbeddedAppearanceDefinitions.RadioColorsDefinitions,
    light: Boolean,
): EmbeddedRowStyle.FlatWithRadio.Colors {
    val colors = if (light) {
        EmbeddedRowStyle.FlatWithRadio.Colors.light()
    } else {
        EmbeddedRowStyle.FlatWithRadio.Colors.dark()
    }
    return colors.apply {
        this@radioColors[definitions.separatorColor]?.let(::separatorColor)
        this@radioColors[definitions.selectedColor]?.let(::selectedColor)
        this@radioColors[definitions.unselectedColor]?.let(::unselectedColor)
    }
}

private fun CheckoutPlaygroundSettings.Snapshot.flatWithCheckmark(
    definitions: CheckoutEmbeddedAppearanceDefinitions.FlatWithCheckmarkDefinitions,
): EmbeddedRowStyle.FlatWithCheckmark {
    val flat = definitions.flat
    return EmbeddedRowStyle.FlatWithCheckmark()
        .separatorThicknessDp(this[flat.separatorThicknessDp])
        .startSeparatorInsetDp(this[flat.startSeparatorInsetDp])
        .endSeparatorInsetDp(this[flat.endSeparatorInsetDp])
        .topSeparatorEnabled(this[flat.topSeparatorEnabled])
        .bottomSeparatorEnabled(this[flat.bottomSeparatorEnabled])
        .checkmarkInsetDp(this[definitions.checkmarkInsetDp])
        .additionalVerticalInsetsDp(this[flat.additionalVerticalInsetsDp])
        .horizontalInsetsDp(this[flat.horizontalInsetsDp])
        .colorsLight(checkmarkColors(definitions.colorsLight, light = true))
        .colorsDark(checkmarkColors(definitions.colorsDark, light = false))
}

private fun CheckoutPlaygroundSettings.Snapshot.checkmarkColors(
    definitions: CheckoutEmbeddedAppearanceDefinitions.CheckmarkColorsDefinitions,
    light: Boolean,
): EmbeddedRowStyle.FlatWithCheckmark.Colors {
    val colors = if (light) {
        EmbeddedRowStyle.FlatWithCheckmark.Colors.light()
    } else {
        EmbeddedRowStyle.FlatWithCheckmark.Colors.dark()
    }
    return colors.apply {
        this@checkmarkColors[definitions.separatorColor]?.let(::separatorColor)
        this@checkmarkColors[definitions.checkmarkColor]?.let(::checkmarkColor)
    }
}

private fun CheckoutPlaygroundSettings.Snapshot.floatingButton(
    definitions: CheckoutEmbeddedAppearanceDefinitions.FloatingButtonDefinitions,
): EmbeddedRowStyle.FloatingButton {
    return EmbeddedRowStyle.FloatingButton()
        .spacingDp(this[definitions.spacingDp])
        .additionalInsetsDp(this[definitions.additionalInsetsDp])
}

private fun CheckoutPlaygroundSettings.Snapshot.flatWithDisclosure(
    definitions: CheckoutEmbeddedAppearanceDefinitions.FlatWithDisclosureDefinitions,
): EmbeddedRowStyle.FlatWithDisclosure {
    val flat = definitions.flat
    return EmbeddedRowStyle.FlatWithDisclosure()
        .separatorThicknessDp(this[flat.separatorThicknessDp])
        .startSeparatorInsetDp(this[flat.startSeparatorInsetDp])
        .endSeparatorInsetDp(this[flat.endSeparatorInsetDp])
        .topSeparatorEnabled(this[flat.topSeparatorEnabled])
        .bottomSeparatorEnabled(this[flat.bottomSeparatorEnabled])
        .additionalVerticalInsetsDp(this[flat.additionalVerticalInsetsDp])
        .horizontalInsetsDp(this[flat.horizontalInsetsDp])
        .colorsLight(disclosureColors(definitions.colorsLight, light = true))
        .colorsDark(disclosureColors(definitions.colorsDark, light = false))
        .apply {
            this@flatWithDisclosure[definitions.disclosureIconRes].resourceId?.let(::disclosureIconRes)
        }
}

private fun CheckoutPlaygroundSettings.Snapshot.disclosureColors(
    definitions: CheckoutEmbeddedAppearanceDefinitions.DisclosureColorsDefinitions,
    light: Boolean,
): EmbeddedRowStyle.FlatWithDisclosure.Colors {
    val colors = if (light) {
        EmbeddedRowStyle.FlatWithDisclosure.Colors.light()
    } else {
        EmbeddedRowStyle.FlatWithDisclosure.Colors.dark()
    }
    return colors.apply {
        this@disclosureColors[definitions.separatorColor]?.let(::separatorColor)
        this@disclosureColors[definitions.disclosureColor]?.let(::disclosureColor)
    }
}

private fun CheckoutPlaygroundSettings.Snapshot.paymentGooglePayConfiguration():
    PaymentElement.Configuration.GooglePayConfiguration {
    val definitions = Controller.payment.googlePay
    return PaymentElement.Configuration.GooglePayConfiguration()
        .display(this[definitions.display])
        .buttonType(this[definitions.buttonType])
        .additionalEnabledNetworks(this[definitions.additionalNetworks])
        .apply {
            this@paymentGooglePayConfiguration[definitions.label]?.let(::label)
        }
}

private fun CheckoutPlaygroundSettings.Snapshot.currencySelectorConfiguration():
    CurrencySelectorElement.Configuration {
    val definitions = Controller.currencySelector.appearance
    val appearance = CurrencySelectorElement.Configuration.Appearance()
        .contentVerticalPaddingDp(this[definitions.contentVerticalPaddingDp])
        .apply {
            this@currencySelectorConfiguration[definitions.cornerRadiusDp]?.let(::cornerRadiusDp)
            this@currencySelectorConfiguration[definitions.borderWidthDp]?.let(::borderWidthDp)
            this@currencySelectorConfiguration[definitions.borderColor]?.let(::borderColor)
            this@currencySelectorConfiguration[definitions.background]?.let(::background)
            this@currencySelectorConfiguration[definitions.selectedBackground]?.let(::selectedBackground)
            this@currencySelectorConfiguration[definitions.textColor]?.let(::textColor)
            this@currencySelectorConfiguration[definitions.selectedTextColor]?.let(::selectedTextColor)
            this@currencySelectorConfiguration[definitions.textSecondaryColor]?.let(::textSecondaryColor)
            this@currencySelectorConfiguration[definitions.dangerColor]?.let(::dangerColor)
        }
        .fontResId(this[definitions.fontResId].resourceId)
        .sizeScaleFactor(this[definitions.sizeScaleFactor])
        .labelContent(this[definitions.labelContent])
    return CurrencySelectorElement.Configuration().appearance(appearance)
}

private fun CheckoutPlaygroundSettings.Snapshot.shippingAddressElementConfiguration():
    ShippingAddressElement.Configuration {
    val definitions = Controller.shippingAddress
    return ShippingAddressElement.Configuration()
        .appearance(shippingAppearance())
        .apply {
            this@shippingAddressElementConfiguration[definitions.title]?.let(::title)
            this@shippingAddressElementConfiguration[definitions.buttonTitle]?.let(::buttonTitle)
        }
}

private fun CheckoutPlaygroundSettings.Snapshot.shippingAppearance(): ShippingAddressElement.Configuration.Appearance {
    val definitions = Controller.shippingAddress.appearance
    return ShippingAddressElement.Configuration.Appearance()
        .colorsLight(shippingColors(definitions.colorsLight, light = true))
        .colorsDark(shippingColors(definitions.colorsDark, light = false))
        .themeMode(this[definitions.themeMode])
        .primaryButton(shippingPrimaryButton(definitions.primaryButton))
        .formInsetValues(
            ShippingAddressElement.Configuration.Appearance.Insets(
                startDp = this[definitions.formInsetValues.startDp],
                topDp = this[definitions.formInsetValues.topDp],
                endDp = this[definitions.formInsetValues.endDp],
                bottomDp = this[definitions.formInsetValues.bottomDp],
            )
        )
        .shapes(
            ShippingAddressElement.Configuration.Appearance.Shapes()
                .cornerRadiusDp(this[definitions.shapes.cornerRadiusDp])
                .borderStrokeWidthDp(this[definitions.shapes.borderStrokeWidthDp])
                .apply {
                    this@shippingAppearance[definitions.shapes.bottomSheetCornerRadiusDp]
                        ?.let(::bottomSheetCornerRadiusDp)
                }
        )
        .typography(
            ShippingAddressElement.Configuration.Appearance.Typography()
                .sizeScaleFactor(this[definitions.typography.sizeScaleFactor])
                .fontResId(this[definitions.typography.fontResId].resourceId)
                .custom(
                    ShippingAddressElement.Configuration.Appearance.Typography.Custom()
                        .h1(shippingFont(definitions.typography.custom.h1))
                )
        )
}

private fun CheckoutPlaygroundSettings.Snapshot.shippingColors(
    definitions: CheckoutColorsDefinitions,
    light: Boolean,
): ShippingAddressElement.Configuration.Appearance.Colors {
    val colors = if (light) {
        ShippingAddressElement.Configuration.Appearance.Colors.light()
    } else {
        ShippingAddressElement.Configuration.Appearance.Colors.dark()
    }
    return colors.apply {
        this@shippingColors[definitions.primary]?.let(::primary)
        this@shippingColors[definitions.surface]?.let(::surface)
        this@shippingColors[definitions.component]?.let(::component)
        this@shippingColors[definitions.componentBorder]?.let(::componentBorder)
        this@shippingColors[definitions.componentDivider]?.let(::componentDivider)
        this@shippingColors[definitions.onComponent]?.let(::onComponent)
        this@shippingColors[definitions.subtitle]?.let(::subtitle)
        this@shippingColors[definitions.placeholderText]?.let(::placeholderText)
        this@shippingColors[definitions.onSurface]?.let(::onSurface)
        this@shippingColors[definitions.appBarIcon]?.let(::appBarIcon)
        this@shippingColors[definitions.error]?.let(::error)
    }
}

private fun CheckoutPlaygroundSettings.Snapshot.shippingPrimaryButton(
    definitions: CheckoutPrimaryButtonDefinitions,
): ShippingAddressElement.Configuration.Appearance.PrimaryButton {
    return ShippingAddressElement.Configuration.Appearance.PrimaryButton()
        .colorsLight(shippingPrimaryButtonColors(definitions.colorsLight, light = true))
        .colorsDark(shippingPrimaryButtonColors(definitions.colorsDark, light = false))
        .shape(
            ShippingAddressElement.Configuration.Appearance.PrimaryButton.Shape()
                .cornerRadiusDp(this[definitions.shape.cornerRadiusDp])
                .borderStrokeWidthDp(this[definitions.shape.borderStrokeWidthDp])
                .heightDp(this[definitions.shape.heightDp])
        )
        .typography(
            ShippingAddressElement.Configuration.Appearance.PrimaryButton.Typography()
                .fontResId(this[definitions.typography.fontResId].resourceId)
                .fontSizeSp(this[definitions.typography.fontSizeSp])
        )
}

private fun CheckoutPlaygroundSettings.Snapshot.shippingPrimaryButtonColors(
    definitions: CheckoutPrimaryButtonDefinitions.ColorsDefinitions,
    light: Boolean,
): ShippingAddressElement.Configuration.Appearance.PrimaryButton.Colors {
    val colors = if (light) {
        ShippingAddressElement.Configuration.Appearance.PrimaryButton.Colors.light()
    } else {
        ShippingAddressElement.Configuration.Appearance.PrimaryButton.Colors.dark()
    }
    return colors.apply {
        this@shippingPrimaryButtonColors[definitions.background]?.let(::background)
        this@shippingPrimaryButtonColors[definitions.onBackground]?.let(::onBackground)
        this@shippingPrimaryButtonColors[definitions.border]?.let(::border)
    }
}

private fun CheckoutPlaygroundSettings.Snapshot.shippingFont(
    definitions: CheckoutFontDefinitions,
): ShippingAddressElement.Configuration.Appearance.Typography.Font? {
    if (!definitions.isSet(this)) return null
    return ShippingAddressElement.Configuration.Appearance.Typography.Font()
        .fontFamily(this[definitions.fontFamily].resourceId)
        .fontSizeSp(this[definitions.fontSizeSp])
        .fontWeight(this[definitions.fontWeight])
        .letterSpacingSp(this[definitions.letterSpacingSp])
}

private fun CheckoutPlaygroundSettings.Snapshot.expressCheckoutConfiguration(): ExpressCheckoutElement.Configuration {
    val appearance = Controller.express.appearance
    return ExpressCheckoutElement.Configuration()
        .linkConfiguration(
            ExpressCheckoutElement.Configuration.LinkConfiguration()
                .display(this[Controller.express.link.display])
        )
        .googlePayConfiguration(expressGooglePayConfiguration())
        .appearance(
            ExpressCheckoutElement.Configuration.Appearance()
                .buttonLayout(
                    ExpressCheckoutElement.Configuration.Appearance.ButtonLayout()
                        .maxColumns(this[appearance.buttonLayout.maxColumns])
                        .maxRows(this[appearance.buttonLayout.maxRows])
                )
                .buttonTheme(this[appearance.buttonTheme])
                .cornerRadiusDp(this[appearance.cornerRadiusDp])
        )
}

private fun CheckoutPlaygroundSettings.Snapshot.expressGooglePayConfiguration():
    ExpressCheckoutElement.Configuration.GooglePayConfiguration {
    val definitions = Controller.express.googlePay
    return ExpressCheckoutElement.Configuration.GooglePayConfiguration()
        .display(this[definitions.display])
        .buttonType(this[definitions.buttonType])
        .additionalEnabledNetworks(this[definitions.additionalNetworks])
        .apply {
            this@expressGooglePayConfiguration[definitions.label]?.let(::label)
        }
}
