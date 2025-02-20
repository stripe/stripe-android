package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.Experience
import com.stripe.android.financialconnections.example.Flow
import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.Merchant
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

data class TestModeSetting(
    override val selectedOption: Boolean = false,
    override val key: String = "financial_connections_test_mode",
) : Saveable<Boolean>, SingleChoiceSetting<Boolean>(
    displayName = "Enable Test Mode",
    options = listOf(
        Option("On", true),
        Option("Off", false),
    ),
    selectedOption = selectedOption
) {
    override fun lasRequest(
        body: LinkAccountSessionBody,
    ): LinkAccountSessionBody = body.copy(testMode = selectedOption)

    override fun paymentIntentRequest(
        body: PaymentIntentBody,
    ): PaymentIntentBody = body.copy(testMode = selectedOption)

    override fun valueUpdated(currentSettings: List<Setting<*>>, value: Boolean): List<Setting<*>> {
        return replace(currentSettings, this.copy(selectedOption = value))
    }

    override fun shouldDisplay(
        merchant: Merchant,
        flow: Flow,
        experience: Experience,
    ): Boolean {
        return merchant.canSwitchBetweenTestAndLive
    }

    override fun convertToValue(value: String): Boolean = value.toBoolean()

    override fun convertToString(value: Boolean): String = value.toString()
}
