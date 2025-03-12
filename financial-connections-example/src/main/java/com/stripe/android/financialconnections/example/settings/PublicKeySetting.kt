package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.Experience
import com.stripe.android.financialconnections.example.Flow
import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.Merchant
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

internal data class PublicKeySetting(
    override val selectedOption: String = "",
    override val key: String = "pk"
) : Saveable<String>, SingleChoiceSetting<String>(
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
