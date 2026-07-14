package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.Experience
import com.stripe.android.financialconnections.example.Flow
import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.Merchant
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

data class RequireAccountNumbersSetting(
    override val selectedOption: String? = null,
    override val key: String = "require_account_numbers",
) : Saveable<String?>, SingleChoiceSetting<String?>(
    displayName = "Require account numbers",
    options = listOf(
        Option("unset", null),
        Option("none", "none"),
        Option("at_least_one", "at_least_one"),
    ),
    selectedOption = selectedOption
) {
    override fun lasRequest(body: LinkAccountSessionBody): LinkAccountSessionBody = body.copy(
        requireAccountNumbers = selectedOption,
    )

    override fun paymentIntentRequest(body: PaymentIntentBody): PaymentIntentBody = body

    override fun shouldDisplay(merchant: Merchant, flow: Flow, experience: Experience): Boolean =
        experience == Experience.FinancialConnections

    override fun valueUpdated(currentSettings: List<Setting<*>>, value: String?): List<Setting<*>> =
        replace(currentSettings, this.copy(selectedOption = value))

    override fun convertToString(value: String?): String = value ?: ""
    override fun convertToValue(value: String): String? = value.takeIf { it.isNotEmpty() }
}
