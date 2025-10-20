package com.stripe.android.paymentsheet.example.playground.settings

internal object CustomerSessionOnBehalfOfSettingsDefinition :
    PlaygroundSettingDefinition<CustomerSessionOnBehalfOfSettingsDefinition.OnBehalfOf>,
    PlaygroundSettingDefinition.Saveable<CustomerSessionOnBehalfOfSettingsDefinition.OnBehalfOf>,
    PlaygroundSettingDefinition.Displayable<CustomerSessionOnBehalfOfSettingsDefinition.OnBehalfOf> {

    private const val US_CONNECTED_ACCOUNT_ID = "acct_1SG4B2LapbQGsfjG"
    private const val FR_CONNECTED_ACCOUNT_ID = "acct_1SGP1sPvdtoA7EjP"

    override val key: String = "onBehalfOfAccountSetting"
    override val defaultValue: OnBehalfOf = OnBehalfOf.NO_CONNECTED_ACCOUNT

    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType.isCustomerFlow()
    }

    override fun convertToString(value: OnBehalfOf): String {
        return value.value
    }

    override fun convertToValue(value: String): OnBehalfOf {
        return when (value) {
            US_CONNECTED_ACCOUNT_ID -> OnBehalfOf.US_CONNECTED_ACCOUNT
            FR_CONNECTED_ACCOUNT_ID -> OnBehalfOf.FR_CONNECTED_ACCOUNT
            else -> OnBehalfOf.NO_CONNECTED_ACCOUNT
        }
    }

    override fun valueUpdated(value: OnBehalfOf, playgroundSettings: PlaygroundSettings) {
        when (value) {
            OnBehalfOf.US_CONNECTED_ACCOUNT,
            OnBehalfOf.FR_CONNECTED_ACCOUNT -> {
                // This setting is only available with CustomerSession
                playgroundSettings[CustomerSessionSettingsDefinition] = true

                // Only the US platform account is configured with connected accounts
                playgroundSettings[CountrySettingsDefinition] = Country.US
            }
            else -> {
                // Do nothing
            }
        }
    }

    override val displayName: String = "Customer Session OnBehalfOf Connected Account"

    override fun createOptions(configurationData: PlaygroundConfigurationData):
        List<PlaygroundSettingDefinition.Displayable.Option<OnBehalfOf>> {
        return listOf(
            PlaygroundSettingDefinition.Displayable.Option("None", OnBehalfOf.NO_CONNECTED_ACCOUNT),
            PlaygroundSettingDefinition.Displayable.Option("USA", OnBehalfOf.US_CONNECTED_ACCOUNT),
            PlaygroundSettingDefinition.Displayable.Option("France", OnBehalfOf.FR_CONNECTED_ACCOUNT)
        )
    }

    enum class OnBehalfOf(override val value: String) : ValueEnum {
        NO_CONNECTED_ACCOUNT(""),
        US_CONNECTED_ACCOUNT(US_CONNECTED_ACCOUNT_ID),
        FR_CONNECTED_ACCOUNT(FR_CONNECTED_ACCOUNT_ID),
    }
}
