package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.Experience
import com.stripe.android.financialconnections.example.Flow
import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.Merchant
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

internal data class StripeAccountIdSetting(
    override val selectedOption: String = "",
    override val key: String = "stripe_account_id",
) : Saveable<String>, SingleChoiceSetting<String>(
    displayName = "StripeAccountId",
    options = emptyList(),
    selectedOption = selectedOption
) {
    override fun lasRequest(body: LinkAccountSessionBody): LinkAccountSessionBody = body.copy(
        stripeAccountId = selectedOption
    )

    override fun paymentIntentRequest(body: PaymentIntentBody): PaymentIntentBody = body.copy(
        stripeAccountId = selectedOption
    )

    override fun shouldDisplay(merchant: Merchant, flow: Flow, experience: Experience): Boolean {
        return experience == Experience.FinancialConnections
    }

    override fun valueUpdated(
        currentSettings: List<Setting<*>>,
        value: String
    ): List<Setting<*>> {
        return replace(currentSettings, this.copy(selectedOption = value))
    }

    override fun convertToString(value: String): String = value
    override fun convertToValue(value: String): String = value
}
