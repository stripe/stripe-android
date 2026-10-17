package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettingDefinition.Displayable.Option

internal object UserCountryOverrideSettingsDefinition :
    PlaygroundSettingDefinition<UserOverrideCountry>,
    PlaygroundSettingDefinition.Saveable<UserOverrideCountry>,
    PlaygroundSettingDefinition.Displayable<UserOverrideCountry> {

    override val key: String = "user-country-override"
    override val defaultValue: UserOverrideCountry = UserOverrideCountry.Off
    override val displayName: String = "User Country Override"

    override fun convertToValue(value: String): UserOverrideCountry {
        return UserOverrideCountry.entries.find { it.value == value } ?: UserOverrideCountry.Off
    }

    override fun convertToString(value: UserOverrideCountry): String {
        return value.value
    }

    override fun createOptions(configurationData: PlaygroundConfigurationData): List<Option<UserOverrideCountry>> {
        return listOf(
            Option(name = "Off", value = UserOverrideCountry.Off),
            Option(name = "US", value = UserOverrideCountry.US),
            Option(name = "GB", value = UserOverrideCountry.GB),
        )
    }

    override fun configure(
        value: UserOverrideCountry,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        val countryOverride = value.value.takeIf { value != UserOverrideCountry.Off }
        configurationBuilder.userOverrideCountry(countryOverride)
    }

    override fun configure(
        value: UserOverrideCountry,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        val countryOverride = value.value.takeIf { value != UserOverrideCountry.Off }
        configurationBuilder.userOverrideCountry(countryOverride)
    }
}

enum class UserOverrideCountry(override val value: String) : ValueEnum {
    Off("off"),
    US("US"),
    GB("GB"),
}
