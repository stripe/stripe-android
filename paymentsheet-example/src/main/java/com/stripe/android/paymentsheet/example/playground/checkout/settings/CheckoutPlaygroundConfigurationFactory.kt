@file:OptIn(
    com.stripe.android.CollectMissingLinkBillingDetailsPreview::class,
    com.stripe.android.LinkDisallowFundingSourceCreationPreview::class,
    com.stripe.android.paymentelement.CheckoutSessionPreview::class,
)

package com.stripe.android.paymentsheet.example.playground.checkout.settings

import com.stripe.android.checkout.CheckoutController
import com.stripe.android.elements.CurrencySelectorElement
import com.stripe.android.elements.ExpressCheckoutElement
import com.stripe.android.elements.PaymentElement
import com.stripe.android.elements.ShippingAddressElement
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundDefinitions.Controller

internal fun CheckoutPlaygroundSettings.Snapshot.checkoutControllerConfiguration(): CheckoutController.Configuration {
    return CheckoutController.Configuration()
        .defaults(defaults())
        .apply {
            if (this@checkoutControllerConfiguration[Controller.payment.shouldSetConfiguration]) {
                paymentElement(paymentElementConfiguration())
            }
            if (this@checkoutControllerConfiguration[Controller.currencySelector.shouldSetConfiguration]) {
                currencySelectorElement(currencySelectorConfiguration())
            }
            if (this@checkoutControllerConfiguration[Controller.shippingAddress.shouldSetConfiguration]) {
                shippingAddressElement(shippingAddressElementConfiguration())
            }
            if (this@checkoutControllerConfiguration[Controller.express.shouldSetConfiguration]) {
                expressCheckoutElement(expressCheckoutConfiguration())
            }
            this@checkoutControllerConfiguration[Controller.merchantDisplayName]?.let(::merchantDisplayName)
        }
}

private fun CheckoutPlaygroundSettings.Snapshot.defaults(): CheckoutController.Configuration.Defaults {
    return CheckoutController.Configuration.Defaults()
        .email(this[Controller.defaults.email])
        .apply {
            contactDetails(Controller.defaults.billing)?.let(::billingDetails)
            contactDetails(Controller.defaults.shipping)?.let(::shippingDetails)
        }
}

private fun CheckoutPlaygroundSettings.Snapshot.contactDetails(
    definitions: CheckoutContactDetailsDefinitions,
): CheckoutController.Configuration.Defaults.ContactDetails? {
    if (!this[definitions.enabled]) return null
    return CheckoutController.Configuration.Defaults.ContactDetails()
        .name(this[definitions.name])
        .apply {
            if (this@contactDetails[definitions.address.enabled]) {
                address(
                    CheckoutController.Address()
                        .country(this@contactDetails[definitions.address.country].uppercase())
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
    return PaymentElement.Configuration.Appearance()
        .colorsLight(paymentColors(Controller.payment.appearance.lightColors, light = true))
        .colorsDark(paymentColors(Controller.payment.appearance.darkColors, light = false))
        .themeMode(this[Controller.payment.appearance.themeMode])
        .primaryButton(primaryButtonAppearance())
        .formInsetValues(
            PaymentElement.Configuration.Appearance.Insets(
                horizontalDp = this[Controller.payment.appearance.insets.horizontal],
                verticalDp = this[Controller.payment.appearance.insets.vertical],
            )
        )
}

private fun CheckoutPlaygroundSettings.Snapshot.paymentColors(
    definitions: CheckoutPaymentColorsDefinitions,
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

private fun CheckoutPlaygroundSettings.Snapshot.primaryButtonAppearance():
    PaymentElement.Configuration.Appearance.PrimaryButton {
    return PaymentElement.Configuration.Appearance.PrimaryButton()
        .colorsLight(primaryButtonColors(Controller.payment.appearance.primaryButton.lightColors, light = true))
        .colorsDark(primaryButtonColors(Controller.payment.appearance.primaryButton.darkColors, light = false))
        .shape(
            PaymentElement.Configuration.Appearance.PrimaryButton.Shape()
                .cornerRadiusDp(this[Controller.payment.appearance.primaryButton.shape.cornerRadius])
                .borderStrokeWidthDp(this[Controller.payment.appearance.primaryButton.shape.borderWidth])
                .heightDp(this[Controller.payment.appearance.primaryButton.shape.height])
        )
        .typography(
            PaymentElement.Configuration.Appearance.PrimaryButton.Typography()
                .fontResId(this[Controller.payment.appearance.primaryButton.typography.font].resourceId)
                .fontSizeSp(this[Controller.payment.appearance.primaryButton.typography.size])
        )
}

private fun CheckoutPlaygroundSettings.Snapshot.primaryButtonColors(
    definitions: CheckoutPrimaryButtonColorsDefinitions,
    light: Boolean,
): PaymentElement.Configuration.Appearance.PrimaryButton.Colors {
    val colors = if (light) {
        PaymentElement.Configuration.Appearance.PrimaryButton.Colors.light()
    } else {
        PaymentElement.Configuration.Appearance.PrimaryButton.Colors.dark()
    }
    return colors.apply {
        this@primaryButtonColors[definitions.background]?.let(::background)
        this@primaryButtonColors[definitions.onBackground]?.let(::onBackground)
        this@primaryButtonColors[definitions.border]?.let(::border)
        this@primaryButtonColors[definitions.successBackground]?.let(::successBackgroundColor)
        this@primaryButtonColors[definitions.onSuccessBackground]?.let(::onSuccessBackgroundColor)
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
        .contentVerticalPaddingDp(this[definitions.verticalPadding])
        .sizeScaleFactor(this[definitions.scale])
        .labelContent(this[definitions.label])
        .fontResId(this[definitions.font].resourceId)
        .apply {
            this@currencySelectorConfiguration[definitions.cornerRadius]?.let(::cornerRadiusDp)
            this@currencySelectorConfiguration[definitions.borderWidth]?.let(::borderWidthDp)
            this@currencySelectorConfiguration[definitions.borderColor]?.let(::borderColor)
            this@currencySelectorConfiguration[definitions.background]?.let(::background)
            this@currencySelectorConfiguration[definitions.selectedBackground]?.let(::selectedBackground)
            this@currencySelectorConfiguration[definitions.textColor]?.let(::textColor)
            this@currencySelectorConfiguration[definitions.selectedTextColor]?.let(::selectedTextColor)
            this@currencySelectorConfiguration[definitions.secondaryTextColor]?.let(::textSecondaryColor)
            this@currencySelectorConfiguration[definitions.dangerColor]?.let(::dangerColor)
        }
    return CurrencySelectorElement.Configuration().appearance(appearance)
}

private fun CheckoutPlaygroundSettings.Snapshot.shippingAddressElementConfiguration():
    ShippingAddressElement.Configuration {
    return ShippingAddressElement.Configuration().appearance(shippingAppearance())
}

private fun CheckoutPlaygroundSettings.Snapshot.shippingAppearance(): ShippingAddressElement.Configuration.Appearance {
    val definitions = Controller.shippingAddress.appearance
    return ShippingAddressElement.Configuration.Appearance()
        .colorsLight(shippingColors(definitions.lightColors, light = true))
        .colorsDark(shippingColors(definitions.darkColors, light = false))
        .themeMode(this[definitions.themeMode])
        .primaryButton(shippingPrimaryButtonAppearance())
        .formInsetValues(
            ShippingAddressElement.Configuration.Appearance.Insets(
                startDp = this[definitions.insets.start],
                topDp = this[definitions.insets.top],
                endDp = this[definitions.insets.end],
                bottomDp = this[definitions.insets.bottom],
            )
        )
}

private fun CheckoutPlaygroundSettings.Snapshot.shippingColors(
    definitions: CheckoutPaymentColorsDefinitions,
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

private fun CheckoutPlaygroundSettings.Snapshot.shippingPrimaryButtonAppearance():
    ShippingAddressElement.Configuration.Appearance.PrimaryButton {
    val definitions = Controller.shippingAddress.appearance.primaryButton
    return ShippingAddressElement.Configuration.Appearance.PrimaryButton()
        .colorsLight(shippingPrimaryButtonColors(definitions.lightColors, light = true))
        .colorsDark(shippingPrimaryButtonColors(definitions.darkColors, light = false))
        .shape(
            ShippingAddressElement.Configuration.Appearance.PrimaryButton.Shape()
                .cornerRadiusDp(this[definitions.shape.cornerRadius])
                .borderStrokeWidthDp(this[definitions.shape.borderWidth])
                .heightDp(this[definitions.shape.height])
        )
        .typography(
            ShippingAddressElement.Configuration.Appearance.PrimaryButton.Typography()
                .fontResId(this[definitions.typography.font].resourceId)
                .fontSizeSp(this[definitions.typography.size])
        )
}

private fun CheckoutPlaygroundSettings.Snapshot.shippingPrimaryButtonColors(
    definitions: CheckoutPrimaryButtonColorsDefinitions,
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

private fun CheckoutPlaygroundSettings.Snapshot.expressCheckoutConfiguration(): ExpressCheckoutElement.Configuration {
    return ExpressCheckoutElement.Configuration()
        .linkConfiguration(
            ExpressCheckoutElement.Configuration.LinkConfiguration()
                .display(this[Controller.express.link.display])
                .collectMissingBillingDetailsForExistingPaymentMethods(
                    this[Controller.express.link.collectMissingBilling]
                )
                .disallowFundingSourceCreation(this[Controller.express.link.disallowedFunding].toSet())
        )
        .googlePayConfiguration(expressGooglePayConfiguration())
        .appearance(
            ExpressCheckoutElement.Configuration.Appearance()
                .buttonTheme(this[Controller.express.appearance.theme])
                .buttonLayout(
                    ExpressCheckoutElement.Configuration.Appearance.ButtonLayout()
                        .maxColumns(this[Controller.express.appearance.layout.columns])
                        .maxRows(this[Controller.express.appearance.layout.rows])
                )
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
