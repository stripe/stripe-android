package com.stripe.android.paymentsheet.example.playground.settings

internal object IntegrationTypeSettingsDefinition :
    PlaygroundSettingDefinition<IntegrationTypeSettingsDefinition.IntegrationType>(
        key = "integrationType",
        displayName = "Integration Type",
    ) {
    override val defaultValue: IntegrationType = IntegrationType.PaymentSheet
    override val options: List<Option<IntegrationType>> = listOf(
        Option("Payment Sheet", IntegrationType.PaymentSheet),
        Option("Flow Controller", IntegrationType.FlowController)
    )

    override fun convertToValue(value: String): IntegrationType {
        return IntegrationType.values().firstOrNull { it.value == value } ?: defaultValue
    }

    override fun convertToString(value: IntegrationType): String {
        return value.value
    }

    enum class IntegrationType(val value: String) {
        PaymentSheet("paymentSheet"), FlowController("flowController")
    }
}
