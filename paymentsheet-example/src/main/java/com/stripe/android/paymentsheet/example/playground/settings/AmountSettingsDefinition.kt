package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object AmountSettingsDefinition :
    PlaygroundSettingDefinition<Amount>,
    PlaygroundSettingDefinition.Saveable<Amount> by EnumSaveable(
        key = "amount",
        values = Amount.entries.toTypedArray(),
        defaultValue = Amount.AMOUNT_5099,
    ),
    PlaygroundSettingDefinition.Displayable<Amount> {
    override val displayName: String = "Amount"

    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        return configurationData.integrationType.isPaymentFlow()
    }

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = Amount.entries.map {
        option(it.displayName, it)
    }

    override fun configure(value: Amount, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.amount(value.longValue)
    }
}

enum class Amount(val displayName: String, override val value: String, val longValue: Long) : ValueEnum {
    AMOUNT_5099("$50.99", "5099", 5099L),
    AMOUNT_10000("$100.00", "10000", 10000L),
}
