package com.stripe.android.paymentsheet.example.playground.settings

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

    override val options: List<PlaygroundSettingDefinition.Displayable.Option<DefaultBillingAddress>>
        get() = listOf(
            option("On", DefaultBillingAddress.On),
            option("On with random email", DefaultBillingAddress.OnWithRandomEmail),
            option("Off", DefaultBillingAddress.Off),
        )

    override fun configure(
        value: DefaultBillingAddress,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        val email = when (value) {
            DefaultBillingAddress.On -> "email@email.com"
            DefaultBillingAddress.OnWithRandomEmail -> "email_${UUID.randomUUID()}@email.com"
            DefaultBillingAddress.Off -> null
        }

        val billingDetails = email?.let {
            PaymentSheet.BillingDetails(
                address = PaymentSheet.Address(
                    line1 = "354 Oyster Point Blvd",
                    line2 = null,
                    city = "South San Francisco",
                    state = "CA",
                    postalCode = "94080",
                    country = "US",
                ),
                email = it,
                name = "Jenny Rosen",
                phone = "+18008675309",
            )
        }

        configurationBuilder.defaultBillingDetails(billingDetails)
    }
}

internal enum class DefaultBillingAddress(override val value: String) : ValueEnum {
    On("on"),
    OnWithRandomEmail("on_with_random_email"),
    Off("off"),
}
