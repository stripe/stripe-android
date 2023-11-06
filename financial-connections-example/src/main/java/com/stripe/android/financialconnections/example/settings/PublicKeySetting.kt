package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

internal data class PublicKeySetting(
    override var selectedOption: String? = null
) : SingleChoiceSetting<String?>(
    displayName = "Publishable Key",
    options = emptyList(),
    selectedOption = selectedOption

) {
    override fun lasRequest(body: LinkAccountSessionBody): LinkAccountSessionBody = body.copy(
        publishableKey = selectedOption
    )

    override fun paymentIntentRequest(body: PaymentIntentBody): PaymentIntentBody = body.copy(
        publishableKey = selectedOption
    )

    override fun valueUpdated(
        currentSettings: List<Setting<*>>,
        value: String?
    ): List<Setting<*>> {
        return replace(currentSettings, this.copy(selectedOption = value))
    }
}