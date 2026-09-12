package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.Experience
import com.stripe.android.financialconnections.example.Flow
import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.Merchant
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

data class ManualEntrySetting(
    override val selectedOption: String? = null,
    override val key: String = "manual_entry_mode",
) : Saveable<String?>, SingleChoiceSetting<String?>(
    displayName = "Manual entry mode",
    options = listOf(
        Option("unset", null),
        Option("automatic", "automatic"),
        Option("disabled", "disabled"),
    ),
    selectedOption = selectedOption
) {
    override fun lasRequest(body: LinkAccountSessionBody): LinkAccountSessionBody = body.copy(
        manualEntryMode = selectedOption,
    )

    override fun paymentIntentRequest(body: PaymentIntentBody): PaymentIntentBody = body

    override fun shouldDisplay(merchant: Merchant, flow: Flow, experience: Experience): Boolean =
        experience == Experience.FinancialConnections

    override fun valueUpdated(currentSettings: List<Setting<*>>, value: String?): List<Setting<*>> =
        replace(currentSettings, this.copy(selectedOption = value))

    override fun convertToString(value: String?): String = value ?: ""
    override fun convertToValue(value: String): String? = value.takeIf { it.isNotEmpty() }
}
