package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.Experience
import com.stripe.android.financialconnections.example.Flow
import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.Merchant
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

internal data class PrivateKeySetting(
    override val selectedOption: String = "",
    override val key: String = "sk"
) : Saveable<String>, SingleChoiceSetting<String>(
    displayName = "Private Key",
    options = emptyList(),
    selectedOption = selectedOption
) {
    override fun lasRequest(body: LinkAccountSessionBody): LinkAccountSessionBody = body.copy(
        secretKey = selectedOption
    )

    override fun paymentIntentRequest(body: PaymentIntentBody): PaymentIntentBody = body.copy(
        secretKey = selectedOption
    )

    override fun valueUpdated(
        currentSettings: List<Setting<*>>,
        value: String
    ): List<Setting<*>> {
        return replace(currentSettings, this.copy(selectedOption = value))
    }

    override fun shouldDisplay(
        merchant: Merchant,
        flow: Flow,
        experience: Experience,
    ): Boolean {
        return merchant.name == "Custom"
    }

    override fun convertToString(value: String): String = value

    override fun convertToValue(value: String): String = value
}
