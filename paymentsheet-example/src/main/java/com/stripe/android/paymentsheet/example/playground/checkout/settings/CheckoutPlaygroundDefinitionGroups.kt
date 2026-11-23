@file:OptIn(com.stripe.android.checkout.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout.settings

import com.stripe.android.elements.PaymentElement
import com.stripe.android.paymentsheet.example.R

internal class CheckoutContactDetailsDefinitions(
    key: String,
    displayName: String,
) {
    val enabled = boolean(
        key = "$key.enabled",
        displayName = "Provide details",
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

internal class CheckoutGooglePayDefinitions<DisplayType : Enum<DisplayType>, ButtonTypeValue : Enum<ButtonTypeValue>>(
    key: String,
    displayName: String,
    displayOptions: List<DisplayType>,
    buttonTypeOptions: List<ButtonTypeValue>,
    defaultDisplay: DisplayType = displayOptions.first { it.name == "Automatic" },
    defaultButtonType: ButtonTypeValue = buttonTypeOptions.first { it.name == "Pay" },
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

internal enum class CheckoutDisclosureIcon(
    val displayName: String,
    val serializedValue: String,
    val resourceId: Int?,
) {
    Default(displayName = "Default", serializedValue = "default", resourceId = null),
    ArrowDown(displayName = "Arrow down", serializedValue = "arrow_down", resourceId = R.drawable.ic_arrow_down),
    Add(displayName = "Add", serializedValue = "add", resourceId = R.drawable.ic_add_black_32dp),
}
