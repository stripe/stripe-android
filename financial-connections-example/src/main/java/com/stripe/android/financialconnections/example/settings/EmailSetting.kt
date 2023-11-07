package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

internal data class EmailSetting(
    override val selectedOption: String = ""
) : SingleChoiceSetting<String>(
    displayName = "Customer email",
    options = emptyList(),
    selectedOption = selectedOption
) {
    override fun lasRequest(body: LinkAccountSessionBody): LinkAccountSessionBody = body.copy(
        customerEmail = selectedOption
    )

    override fun paymentIntentRequest(body: PaymentIntentBody): PaymentIntentBody = body.copy(
        customerEmail = selectedOption
    )

    override fun valueUpdated(
        currentSettings: List<Setting<*>>,
        value: String
    ): List<Setting<*>> {
        return replace(currentSettings, this.copy(selectedOption = value))
    }
}
