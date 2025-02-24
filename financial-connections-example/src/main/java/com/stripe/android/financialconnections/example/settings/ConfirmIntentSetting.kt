package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.Experience
import com.stripe.android.financialconnections.example.Flow
import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.Merchant
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

data class ConfirmIntentSetting(
    override val selectedOption: Boolean = false,
    override val key: String = "financial_connections_confirm_intent",
) : Saveable<Boolean>, SingleChoiceSetting<Boolean>(
    displayName = "Confirm Intent",
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
        return replace(currentSettings, this.copy(selectedOption = value))
    }

    override fun shouldDisplay(
        merchant: Merchant,
        flow: Flow,
        experience: Experience,
    ): Boolean {
        return experience == Experience.InstantDebits ||
            experience == Experience.LinkCardBrand ||
            flow == Flow.PaymentIntent
    }

    override fun convertToValue(value: String): Boolean = value.toBoolean()

    override fun convertToString(value: Boolean): String = value.toString()
}
