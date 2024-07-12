package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.Experience
import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

data class ExperienceSetting(
    override val selectedOption: Experience = Experience.FinancialConnections,
    override val key: String = "experience",
) : Saveable<Experience>, SingleChoiceSetting<Experience>(
    displayName = "Experience",
    options = Experience.entries.map { Option(it.displayName, it) },
    selectedOption = selectedOption,
) {
    override fun lasRequest(body: LinkAccountSessionBody): LinkAccountSessionBody = body

    override fun paymentIntentRequest(body: PaymentIntentBody): PaymentIntentBody = body

    override fun convertToString(value: Experience): String {
        return value.name
    }

    override fun convertToValue(value: String): Experience {
        return Experience.entries.find { it.name == value } ?: Experience.FinancialConnections
    }

    override fun valueUpdated(currentSettings: List<Setting<*>>, value: Experience): List<Setting<*>> {
        return replace(currentSettings, this.copy(selectedOption = value))
    }
}
