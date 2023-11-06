package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

internal data class PrivateKeySetting(
    override var selectedOption: String? = null
) : SingleChoiceSetting<String?>(
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
        value: String?
    ): List<Setting<*>> {
        return replace(currentSettings, this.copy(selectedOption = value))
    }
}