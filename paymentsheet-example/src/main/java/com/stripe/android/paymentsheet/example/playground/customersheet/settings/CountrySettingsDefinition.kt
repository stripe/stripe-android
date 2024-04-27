package com.stripe.android.paymentsheet.example.playground.customersheet.settings

import com.stripe.android.core.model.CountryUtils
import com.stripe.android.paymentsheet.example.playground.customersheet.model.CustomerEphemeralKeyRequest
import java.util.Locale

internal object CountrySettingsDefinition :
    CustomerSheetPlaygroundSettingDefinition<Country>,
    CustomerSheetPlaygroundSettingDefinition.Saveable<Country> by EnumSaveable(
        key = "country",
        values = Country.entries.toTypedArray(),
        defaultValue = Country.US,
    ),
    CustomerSheetPlaygroundSettingDefinition.Displayable<Country> {
    private val supportedCountries = Country.entries.map { it.value }.toSet()

    override val displayName: String = "Merchant Country"

    override val options: List<CustomerSheetPlaygroundSettingDefinition.Displayable.Option<Country>>
        get() = CountryUtils.getOrderedCountries(Locale.getDefault()).filter { country ->
            country.code.value in supportedCountries
        }.map { country ->
            option(country.name, convertToValue(country.code.value))
        }.toList()

    override fun configure(value: Country, requestBuilder: CustomerEphemeralKeyRequest.Builder) {
        requestBuilder.merchantCountryCode(value.value)
    }
}

enum class Country(override val value: String) : ValueEnum {
    US("US"),
    FR("FR"),
}
