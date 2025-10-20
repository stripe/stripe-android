package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.link.LinkController
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object AllowedBillingCountriesSettingsDefinition :
    PlaygroundSettingDefinition.Saveable<AllowedBillingCountries> by EnumSaveable(
        key = "allowedBillingCountries",
        defaultValue = AllowedBillingCountries.All,
        values = AllowedBillingCountries.entries.toTypedArray(),
    ),
    PlaygroundSettingDefinition.Displayable<AllowedBillingCountries> {
    override val displayName: String = "Allowed billing countries"

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = listOf(
        option("All", AllowedBillingCountries.All),
        option("US Only", AllowedBillingCountries.UsOnly),
        option("North America (US, CA, MX)", AllowedBillingCountries.NorthAmerica),
        option("5 countries (US, CA, GB, AU, DE)", AllowedBillingCountries.FiveCountries),
    )

    override fun configure(
        value: AllowedBillingCountries,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData,
    ) {
        configurationData.updateBillingDetails { allowedCountries = value.countries }
    }

    override fun configure(
        value: AllowedBillingCountries,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        configurationData.updateBillingDetails { allowedCountries = value.countries }
    }

    override fun configure(
        value: AllowedBillingCountries,
        configurationBuilder: CustomerSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Customer,
        configurationData: PlaygroundSettingDefinition.CustomerSheetConfigurationData,
    ) {
        configurationData.updateBillingDetails { allowedCountries = value.countries }
    }

    override fun configure(
        value: AllowedBillingCountries,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.SharedPaymentToken,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData,
    ) {
        configurationData.updateBillingDetails { allowedCountries = value.countries }
    }

    override fun configure(
        value: AllowedBillingCountries,
        configurationBuilder: LinkController.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.LinkControllerConfigurationData,
    ) {
        configurationData.updateBillingDetails { allowedCountries = value.countries }
    }
}

enum class AllowedBillingCountries(
    override val value: String,
    val countries: Set<String>,
) : ValueEnum {
    All(value = "all", countries = emptySet()),
    UsOnly(value = "us_only", countries = setOf("US")),
    NorthAmerica(value = "north_america", countries = setOf("US", "CA", "MX")),
    FiveCountries(value = "five_countries", countries = setOf("US", "CA", "GB", "AU", "DE"))
}
