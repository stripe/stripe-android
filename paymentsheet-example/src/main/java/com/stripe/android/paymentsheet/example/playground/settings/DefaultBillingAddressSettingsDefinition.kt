package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import java.util.UUID

internal object DefaultBillingAddressSettingsDefinition :
    PlaygroundSettingDefinition<DefaultBillingAddress>,
    PlaygroundSettingDefinition.Saveable<DefaultBillingAddress> by EnumSaveable(
        key = "defaultBillingAddress",
        values = DefaultBillingAddress.entries.toTypedArray(),
        defaultValue = DefaultBillingAddress.On,
    ),
    PlaygroundSettingDefinition.Displayable<DefaultBillingAddress> {

    override val displayName: String
        get() = "Default Billing Address"

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = listOf(
        option("On", DefaultBillingAddress.On),
        option("On with random email", DefaultBillingAddress.OnWithRandomEmail),
        option("Off", DefaultBillingAddress.Off),
    )

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

    @ExperimentalEmbeddedPaymentElementApi
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

    private fun createBillingDetails(value: DefaultBillingAddress): PaymentSheet.BillingDetails? {
        val email = when (value) {
            DefaultBillingAddress.On -> "email@email.com"
            DefaultBillingAddress.OnWithRandomEmail -> "email_${UUID.randomUUID()}@email.com"
            DefaultBillingAddress.Off -> null
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
                phone = "+18008675309",
            )
        }
    }
}

internal enum class DefaultBillingAddress(override val value: String) : ValueEnum {
    On("on"),
    OnWithRandomEmail("on_with_random_email"),
    Off("off"),
}
