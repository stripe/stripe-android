package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.Flow
import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

data class FlowSetting(
    override val selectedOption: Flow = Flow.Data,
    override val key: String = "flow",
) : Saveable<Flow>, SingleChoiceSetting<Flow>(
    displayName = "Flow",
    options = Flow.entries.map { Option(it.name, it) },
    selectedOption = selectedOption
) {
    override fun lasRequest(body: LinkAccountSessionBody): LinkAccountSessionBody = body

    override fun paymentIntentRequest(body: PaymentIntentBody): PaymentIntentBody = body

    override fun convertToString(value: Flow): String {
        return value.apiValue
    }

    override fun convertToValue(value: String): Flow {
        return Flow.fromApiValue(value)
    }

    override fun valueUpdated(currentSettings: List<Setting<*>>, value: Flow): List<Setting<*>> {
        val flowSettings = listOfNotNull(
            copy(selectedOption = value),
            ConfirmIntentSetting().takeIf { value == Flow.PaymentIntent },
        )
        val updatedSettings = currentSettings
            .filter { it !is ConfirmIntentSetting }
            .flatMap { setting ->
                when (setting) {
                    is FlowSetting -> flowSettings
                    else -> listOf(setting)
                }
            }

        return if (currentSettings.none { it is FlowSetting }) {
            updatedSettings + flowSettings
        } else {
            updatedSettings
        }
    }

}
