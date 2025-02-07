package com.stripe.android.financialconnections.example.settings

import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

data class DynamicAppearanceSetting(
    override val selectedOption: Boolean = false,
    override val key: String = "financial_connections_dynamic_appearance",
) : Saveable<Boolean>, SingleChoiceSetting<Boolean>(
    displayName = "Dynamic appearance",
    options = listOf(
        Option("On", true),
        Option("Off", false),
    ),
    selectedOption = selectedOption
) {
    override fun lasRequest(
        body: LinkAccountSessionBody,
    ): LinkAccountSessionBody = body

    override fun paymentIntentRequest(
        body: PaymentIntentBody,
    ): PaymentIntentBody = body

    override fun valueUpdated(currentSettings: List<Setting<*>>, value: Boolean): List<Setting<*>> {
        FeatureFlags.financialConnectionsDarkMode.setEnabled(value)
        return replace(currentSettings, this.copy(selectedOption = value))
    }

    override fun convertToValue(value: String): Boolean = value.toBoolean()

    override fun convertToString(value: Boolean): String = value.toString()
}
