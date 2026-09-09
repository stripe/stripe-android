@file:OptIn(LinkControllerPreview::class)

package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.link.LinkController
import com.stripe.android.link.LinkControllerPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import java.util.UUID

internal object DefaultBillingAddressSettingsDefinition :
    PlaygroundSettingDefinition<DefaultBillingAddress>,
    PlaygroundSettingDefinition.Saveable<DefaultBillingAddress>,
    PlaygroundSettingDefinition.Displayable.WithTextInput<DefaultBillingAddress> {

    override val key: String = "defaultBillingAddress"
    override val defaultValue: DefaultBillingAddress = DefaultBillingAddress.On

    override fun convertToValue(value: String): DefaultBillingAddress {
        if (value.startsWith(CUSTOM_EMAIL_PREFIX)) {
            return DefaultBillingAddress.WithEmail(value.removePrefix(CUSTOM_EMAIL_PREFIX))
        }

        if (value.startsWith(CUSTOM_EMAIL_WITHOUT_PHONE_PREFIX)) {
            return DefaultBillingAddress.WithEmailAndNoPhone(value.removePrefix(CUSTOM_EMAIL_WITHOUT_PHONE_PREFIX))
        }

        return when (value) {
            "on" -> DefaultBillingAddress.On
            "on_with_random_email" -> DefaultBillingAddress.OnWithRandomEmail
            "with_email" -> DefaultBillingAddress.WithEmail("")
            "off" -> DefaultBillingAddress.Off
            else -> defaultValue
        }
    }

    override fun convertToString(value: DefaultBillingAddress): String {
        return when (value) {
            is DefaultBillingAddress.WithEmail -> CUSTOM_EMAIL_PREFIX + value.email
            is DefaultBillingAddress.WithEmailAndNoPhone -> CUSTOM_EMAIL_WITHOUT_PHONE_PREFIX + value.email
            else -> value.value
        }
    }

    override val displayName: String
        get() = "Default Billing Address"

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = listOf(
        option("On", DefaultBillingAddress.On),
        option("On with random email", DefaultBillingAddress.OnWithRandomEmail),
        option("Custom email", DefaultBillingAddress.WithEmail("")),
        option("Off", DefaultBillingAddress.Off),
    )

    override fun optionMatchesValue(
        optionValue: DefaultBillingAddress,
        value: DefaultBillingAddress,
    ): Boolean {
        return optionValue == value ||
            optionValue is DefaultBillingAddress.WithEmail && value is DefaultBillingAddress.WithEmail
    }

    override val textInputName: String = "Custom email"

    override fun textInputValue(value: DefaultBillingAddress): String? {
        return (value as? DefaultBillingAddress.WithEmail)?.email
    }

    override fun updateTextInputValue(
        value: DefaultBillingAddress,
        textInputValue: String,
    ): DefaultBillingAddress {
        return DefaultBillingAddress.WithEmail(textInputValue)
    }

    override fun configure(
        value: DefaultBillingAddress,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        createBillingDetails(value)?.let { billingDetails ->
            configurationBuilder.defaultBillingDetails(billingDetails)
        }
    }

    override fun configure(
        value: DefaultBillingAddress,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        createBillingDetails(value)?.let { billingDetails ->
            configurationBuilder.defaultBillingDetails(billingDetails)
        }
    }

    override fun configure(
        value: DefaultBillingAddress,
        configurationBuilder: CustomerSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Customer,
        configurationData: PlaygroundSettingDefinition.CustomerSheetConfigurationData
    ) {
        createBillingDetails(value)?.let { billingDetails ->
            configurationBuilder.defaultBillingDetails(billingDetails)
        }
    }

    override fun configure(
        value: DefaultBillingAddress,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.SharedPaymentToken,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        createBillingDetails(value)?.let { billingDetails ->
            configurationBuilder.defaultBillingDetails(billingDetails)
        }
    }

    override fun configure(
        value: DefaultBillingAddress,
        configurationBuilder: LinkController.Configuration,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.LinkControllerConfigurationData
    ) {
        createBillingDetails(value)?.let { billingDetails ->
            configurationBuilder.defaultBillingDetails(billingDetails)
        }
    }

    private fun createBillingDetails(value: DefaultBillingAddress): PaymentSheet.BillingDetails? {
        val email = when (value) {
            DefaultBillingAddress.On -> "email@email.com"
            DefaultBillingAddress.OnWithRandomEmail -> "email_${UUID.randomUUID()}@email.com"
            DefaultBillingAddress.Off -> null
            is DefaultBillingAddress.WithEmail -> value.email
            is DefaultBillingAddress.WithEmailAndNoPhone -> value.email
        }

        return email?.let {
            PaymentSheet.BillingDetails(
                address = PaymentSheet.Address(
                    line1 = "354 Oyster Point Blvd",
                    line2 = null,
                    city = "South San Francisco",
                    state = "CA",
                    postalCode = "94080",
                    country = "US",
                ),
                email = email,
                name = "Jenny Rosen",
                phone = if (value is DefaultBillingAddress.WithEmailAndNoPhone) null else "+18008675309",
            )
        }
    }
}

internal sealed class DefaultBillingAddress(val value: String) {
    data object On : DefaultBillingAddress("on")
    data object OnWithRandomEmail : DefaultBillingAddress("on_with_random_email")
    data object Off : DefaultBillingAddress("off")
    data class WithEmail(val email: String) : DefaultBillingAddress("with_email")
    data class WithEmailAndNoPhone(val email: String) : DefaultBillingAddress("with_email_without_phone")
}

private const val CUSTOM_EMAIL_PREFIX = "with_email:"
private const val CUSTOM_EMAIL_WITHOUT_PHONE_PREFIX = "with_email_without_phone:"
