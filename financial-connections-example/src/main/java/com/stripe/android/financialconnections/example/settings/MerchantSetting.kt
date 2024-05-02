package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.Merchant
import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

data class MerchantSetting(
    override val selectedOption: Merchant = Merchant.Default,
    override val key: String = "merchant"
) : Saveable<Merchant>, SingleChoiceSetting<Merchant>(
    displayName = "Merchant",
    options = Merchant.entries.map { Option(it.name, it) },
    selectedOption = selectedOption
) {
    override fun lasRequest(body: LinkAccountSessionBody): LinkAccountSessionBody = body.copy(
        flow = selectedOption.apiValue
    )

    override fun paymentIntentRequest(body: PaymentIntentBody): PaymentIntentBody = body.copy(
        flow = selectedOption.apiValue
    )

    override fun valueUpdated(currentSettings: List<Setting<*>>, value: Merchant): List<Setting<*>> {
        return replace(currentSettings, this.copy(selectedOption = value))
    }

    override fun convertToString(value: Merchant): String = value.apiValue

    override fun convertToValue(value: String): Merchant = Merchant.fromApiValue(value)
}
