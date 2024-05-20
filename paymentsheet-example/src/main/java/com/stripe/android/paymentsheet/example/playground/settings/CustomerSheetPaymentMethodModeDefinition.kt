package com.stripe.android.paymentsheet.example.playground.settings

internal object CustomerSheetPaymentMethodModeDefinition :
    PlaygroundSettingDefinition<PaymentMethodMode>,
    PlaygroundSettingDefinition.Saveable<PaymentMethodMode> by EnumSaveable(
        key = "paymentMethodMode",
        values = PaymentMethodMode.entries.toTypedArray(),
        defaultValue = PaymentMethodMode.SetupIntent,
    ),
    PlaygroundSettingDefinition.Displayable<PaymentMethodMode> {
    override val displayName: String = "Payment Method Mode"

    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType.isCustomerFlow()
    }

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = listOf(
        option("Setup Intent", PaymentMethodMode.SetupIntent),
        option("Create And Attach", PaymentMethodMode.CreateAndAttach),
    )
}

enum class PaymentMethodMode(override val value: String) : ValueEnum {
    SetupIntent("setup_intent"),
    CreateAndAttach("create_and_attach")
}
