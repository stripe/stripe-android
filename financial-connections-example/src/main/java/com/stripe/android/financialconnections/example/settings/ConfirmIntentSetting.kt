package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

data class ConfirmIntentSetting(
    override val selectedOption: Boolean = false,
    override val key: String = "financial_connections_confirm_intent",
) : Saveable<Boolean>, SingleChoiceSetting<Boolean>(
    displayName = "Confirm Intent",
    options = listOf(
        Option("True", true),
        Option("False", false),
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
        return replace(currentSettings, this.copy(selectedOption = value))
    }

    override fun convertToValue(value: String): Boolean = value.toBoolean()

    override fun convertToString(value: Boolean): String = value.toString()
}
