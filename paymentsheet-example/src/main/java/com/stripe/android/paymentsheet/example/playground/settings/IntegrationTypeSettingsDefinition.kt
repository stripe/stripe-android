package com.stripe.android.paymentsheet.example.playground.settings

internal object IntegrationTypeSettingsDefinition :
    PlaygroundSettingDefinition<IntegrationType>,
    PlaygroundSettingDefinition.Saveable<IntegrationType> by EnumSaveable(
        key = "integrationType",
        values = IntegrationType.values(),
        defaultValue = IntegrationType.PaymentSheet,
    ),
    PlaygroundSettingDefinition.Displayable<IntegrationType> {
    override val displayName: String = "Integration Type"
    override val options: List<PlaygroundSettingDefinition.Displayable.Option<IntegrationType>> =
        listOf(
            option("Payment Sheet", IntegrationType.PaymentSheet),
            option("Flow Controller", IntegrationType.FlowController)
        )
}

enum class IntegrationType(override val value: String) : ValueEnum {
    PaymentSheet("paymentSheet"), FlowController("flowController")
}
