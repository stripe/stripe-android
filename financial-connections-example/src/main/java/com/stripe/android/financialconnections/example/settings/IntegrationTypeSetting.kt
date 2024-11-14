package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.IntegrationType
import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

data class IntegrationTypeSetting(
    override val selectedOption: IntegrationType = IntegrationType.Standalone,
    override val key: String = "integration_type",
) : Saveable<IntegrationType>, SingleChoiceSetting<IntegrationType>(
    displayName = "Integration Type",
    options = IntegrationType.entries.map { Option(it.displayName, it) },
    selectedOption = selectedOption,
) {
    override fun lasRequest(body: LinkAccountSessionBody): LinkAccountSessionBody = body

    override fun paymentIntentRequest(body: PaymentIntentBody): PaymentIntentBody = body

    override fun convertToString(value: IntegrationType): String {
        return value.name
    }

    override fun convertToValue(value: String): IntegrationType {
        return IntegrationType.entries.find { it.name == value } ?: IntegrationType.Standalone
    }

    override fun valueUpdated(currentSettings: List<Setting<*>>, value: IntegrationType): List<Setting<*>> {
        return replace(currentSettings, this.copy(selectedOption = value))
    }
}
