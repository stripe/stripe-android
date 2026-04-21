package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

data class ForceNotlinkSetting(
    override val selectedOption: Boolean = false,
    override val key: String = "force_notlink",
) : Saveable<Boolean>, SingleChoiceSetting<Boolean>(
    displayName = "Force Notlink",
    options = listOf(
        Option("Off", false),
        Option("On", true),
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

    override fun convertToValue(value: String): Boolean = value == "true"

    override fun convertToString(value: Boolean): String = value.toString()
}
