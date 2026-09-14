@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout.settings

import com.stripe.android.elements.PaymentElement
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.uicore.StripeColors

internal class CheckoutContactDetailsDefinitions(
    key: String,
    displayName: String,
) {
    val enabled = boolean(
        key = "$key.enabled",
        displayName = "Provide details",
        defaultValue = false,
    )
    val name = optionalText(
        key = "$key.name",
        displayName = "Name",
    )
    val address = CheckoutAddressDefinitions(key = "$key.address")
    val configuration = configuration(
        key = key,
        displayName = displayName,
        children = arrayOf(enabled, name, address.configuration),
    )
}

internal class CheckoutAddressDefinitions(key: String) {
    val enabled = boolean(
        key = "$key.enabled",
        displayName = "Provide address",
        defaultValue = false,
    )
    val country = text(
        key = "$key.country",
        displayName = "Country",
        defaultValue = "US",
        validate = { value ->
            if (value.matches(Regex("[A-Za-z]{2}"))) null else "Use a two-letter country code"
        },
    )
    val city = optionalText(key = "$key.city", displayName = "City")
    val line1 = optionalText(key = "$key.line1", displayName = "Line 1")
    val line2 = optionalText(key = "$key.line2", displayName = "Line 2")
    val postalCode = optionalText(key = "$key.postal_code", displayName = "Postal code")
    val state = optionalText(key = "$key.state", displayName = "State")
    val configuration = configuration(
        key = key,
        displayName = "Address",
        children = arrayOf(
            enabled,
            country,
            city,
            line1,
            line2,
            postalCode,
            state,
        ),
    )
}

internal class CheckoutPaymentColorsDefinitions(
    key: String,
    displayName: String,
    defaultColors: StripeColors,
) {
    val primary = optionalColor(
        key = "$key.primary",
        displayName = "Primary",
        defaultValue = defaultColors.materialColors.primary,
    )
    val surface = optionalColor(
        key = "$key.surface",
        displayName = "Surface",
        defaultValue = defaultColors.materialColors.surface,
    )
    val component = optionalColor(
        key = "$key.component",
        displayName = "Component",
        defaultValue = defaultColors.component,
    )
    val componentBorder = optionalColor(
        key = "$key.component_border",
        displayName = "Component border",
        defaultValue = defaultColors.componentBorder,
    )
    val componentDivider = optionalColor(
        key = "$key.component_divider",
        displayName = "Component divider",
        defaultValue = defaultColors.componentDivider,
    )
    val onComponent = optionalColor(
        key = "$key.on_component",
        displayName = "On component",
        defaultValue = defaultColors.onComponent,
    )
    val subtitle = optionalColor(
        key = "$key.subtitle",
        displayName = "Subtitle",
        defaultValue = defaultColors.subtitle,
    )
    val placeholderText = optionalColor(
        key = "$key.placeholder_text",
        displayName = "Placeholder text",
        defaultValue = defaultColors.placeholderText,
    )
    val onSurface = optionalColor(
        key = "$key.on_surface",
        displayName = "On surface",
        defaultValue = defaultColors.materialColors.onSurface,
    )
    val appBarIcon = optionalColor(
        key = "$key.app_bar_icon",
        displayName = "App bar icon",
        defaultValue = defaultColors.appBarIcon,
    )
    val error = optionalColor(
        key = "$key.error",
        displayName = "Error",
        defaultValue = defaultColors.materialColors.error,
    )
    val configuration = configuration(
        key = key,
        displayName = displayName,
        children = arrayOf(
            primary,
            surface,
            component,
            componentBorder,
            componentDivider,
            onComponent,
            subtitle,
            placeholderText,
            onSurface,
            appBarIcon,
            error,
        ),
    )
}

internal class CheckoutPrimaryButtonColorsDefinitions(
    key: String,
    displayName: String,
) {
    val background = optionalColor(key = "$key.background", displayName = "Background")
    val onBackground = optionalColor(key = "$key.on_background", displayName = "On background")
    val border = optionalColor(key = "$key.border", displayName = "Border")
    val successBackground = optionalColor(key = "$key.success_background", displayName = "Success background")
    val onSuccessBackground = optionalColor(
        key = "$key.on_success_background",
        displayName = "On success background",
    )
    val configuration = configuration(
        key = key,
        displayName = displayName,
        children = arrayOf(
            background,
            onBackground,
            border,
            successBackground,
            onSuccessBackground,
        ),
    )
}

internal class CheckoutGooglePayDefinitions<DisplayType : Enum<DisplayType>, ButtonTypeValue : Enum<ButtonTypeValue>>(
    key: String,
    displayName: String,
    defaultDisplay: DisplayType,
    displayOptions: List<DisplayType>,
    defaultButtonType: ButtonTypeValue,
    buttonTypeOptions: List<ButtonTypeValue>,
) {
    val display = choice(
        key = "$key.display",
        displayName = "Display",
        defaultValue = defaultDisplay,
        options = displayOptions.map { it.name to it },
        serialize = { it.name },
    )
    val label = optionalText(key = "$key.label", displayName = "Label")
    val buttonType = choice(
        key = "$key.button_type",
        displayName = "Button type",
        defaultValue = defaultButtonType,
        options = buttonTypeOptions.map { it.name to it },
        serialize = { it.name },
    )
    val additionalNetworks = stringCsv(
        key = "$key.additional_networks",
        displayName = "Additional networks (comma separated)",
    )
    val configuration = configuration(
        key = key,
        displayName = displayName,
        children = arrayOf(display, label, buttonType, additionalNetworks),
    )
}

internal enum class CheckoutCardBrandAcceptanceMode {
    All,
    Allowed,
    Disallowed,
}

internal enum class CheckoutTermsDisplay(
    val displayName: String,
    val serializedValue: String,
    val configurationValue: PaymentElement.Configuration.TermsDisplay?,
) {
    Default(
        displayName = "Default",
        serializedValue = "Default",
        configurationValue = null,
    ),
    Automatic(
        displayName = "Automatic",
        serializedValue = PaymentElement.Configuration.TermsDisplay.AUTOMATIC.name,
        configurationValue = PaymentElement.Configuration.TermsDisplay.AUTOMATIC,
    ),
    Never(
        displayName = "Never",
        serializedValue = PaymentElement.Configuration.TermsDisplay.NEVER.name,
        configurationValue = PaymentElement.Configuration.TermsDisplay.NEVER,
    ),
}

internal enum class CheckoutFont(
    val serializedValue: String,
    val resourceId: Int?,
) {
    Default(serializedValue = "default", resourceId = null),
    Cursive(serializedValue = "cursive", resourceId = R.font.cursive),
    OpenSans(serializedValue = "opensans", resourceId = R.font.opensans),
}
