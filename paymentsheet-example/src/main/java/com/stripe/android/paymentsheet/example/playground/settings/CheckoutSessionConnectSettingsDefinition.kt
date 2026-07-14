package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object CheckoutSessionConnectSettingsDefinition :
    PlaygroundSettingDefinition<CheckoutSessionConnectSettingsDefinition.ConnectMode>,
    PlaygroundSettingDefinition.Saveable<CheckoutSessionConnectSettingsDefinition.ConnectMode>,
    PlaygroundSettingDefinition.Displayable<CheckoutSessionConnectSettingsDefinition.ConnectMode> {

    const val CONNECTED_ACCOUNT_ID = "acct_1SGP1sPvdtoA7EjP"
    private const val APPLICATION_FEE_AMOUNT = 400L

    override val key: String = "checkoutSessionConnectMode"
    override val defaultValue: ConnectMode = ConnectMode.NONE

    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        return settings[InitializationTypeSettingsDefinition] == InitializationType.CheckoutSession
    }

    override fun convertToString(value: ConnectMode): String = value.value

    override fun convertToValue(value: String): ConnectMode {
        return ConnectMode.entries.firstOrNull { it.value == value } ?: ConnectMode.NONE
    }

    override fun configure(value: ConnectMode, checkoutRequestBuilder: CheckoutRequest.Builder) {
        when (value) {
            ConnectMode.NONE -> { }
            ConnectMode.ON_BEHALF_OF -> {
                checkoutRequestBuilder.onBehalfOf(CONNECTED_ACCOUNT_ID)
            }
            ConnectMode.DESTINATION -> {
                checkoutRequestBuilder.transferDataDestination(CONNECTED_ACCOUNT_ID)
                checkoutRequestBuilder.applicationFeeAmount(APPLICATION_FEE_AMOUNT)
            }
            ConnectMode.DIRECT -> {
                checkoutRequestBuilder.stripeAccountId(CONNECTED_ACCOUNT_ID)
            }
        }
    }

    override val displayName: String = "Checkout Connect Mode"

    override fun createOptions(
        configurationData: PlaygroundConfigurationData,
    ): List<PlaygroundSettingDefinition.Displayable.Option<ConnectMode>> {
        return listOf(
            PlaygroundSettingDefinition.Displayable.Option("None", ConnectMode.NONE),
            PlaygroundSettingDefinition.Displayable.Option("On Behalf Of", ConnectMode.ON_BEHALF_OF),
            PlaygroundSettingDefinition.Displayable.Option("Destination", ConnectMode.DESTINATION),
            PlaygroundSettingDefinition.Displayable.Option("Direct", ConnectMode.DIRECT),
        )
    }

    enum class ConnectMode(override val value: String) : ValueEnum {
        NONE("none"),
        ON_BEHALF_OF("on_behalf_of"),
        DESTINATION("destination"),
        DIRECT("direct"),
    }
}
