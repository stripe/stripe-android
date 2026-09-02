package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.Experience
import com.stripe.android.financialconnections.example.Flow
import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.Merchant
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

data class AccountsLimitSetting(
    override val selectedOption: String = "",
    override val key: String = "accounts_limit",
) : Saveable<String>, SingleChoiceSetting<String>(
    displayName = "Maximum accounts",
    options = emptyList(),
    selectedOption = selectedOption
) {
    override fun lasRequest(body: LinkAccountSessionBody): LinkAccountSessionBody = body.copy(
        accountsLimit = selectedOption.toIntOrNull()?.takeIf { it > 0 },
    )

    override fun paymentIntentRequest(body: PaymentIntentBody): PaymentIntentBody = body

    override fun shouldDisplay(merchant: Merchant, flow: Flow, experience: Experience): Boolean =
        experience == Experience.FinancialConnections

    override fun valueUpdated(currentSettings: List<Setting<*>>, value: String): List<Setting<*>> =
        replace(currentSettings, this.copy(selectedOption = value.filter { it.isDigit() }))

    override fun convertToString(value: String): String = value
    override fun convertToValue(value: String): String = value
}
