package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.Merchant
import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

data class MerchantSetting(
    override val selectedOption: Merchant = Merchant.Test,
    override val key: String = "merchant"
) : Saveable<Merchant>, SingleChoiceSetting<Merchant>(
    displayName = "Merchant",
    options = Merchant.values().map { Option(it.name, it) },
    selectedOption = selectedOption
) {
    override fun lasRequest(body: LinkAccountSessionBody): LinkAccountSessionBody = body.copy(
        flow = selectedOption.apiValue
    )

    override fun paymentIntentRequest(body: PaymentIntentBody): PaymentIntentBody = body.copy(
        flow = selectedOption.apiValue
    )

    override fun valueUpdated(currentSettings: List<Setting<*>>, value: Merchant): List<Setting<*>> {
        val merchantSettings = listOfNotNull(
            copy(selectedOption = value),
            PublicKeySetting("").takeIf { value == Merchant.Other },
            PrivateKeySetting("").takeIf { value == Merchant.Other }
        )
        val updatedSettings = currentSettings
            .filter { it !is PublicKeySetting && it !is PrivateKeySetting }
            .flatMap { setting ->
                when (setting) {
                    is MerchantSetting -> merchantSettings
                    else -> listOf(setting)
                }
            }

        return if (currentSettings.none { it is MerchantSetting }) {
            updatedSettings + merchantSettings
        } else {
            updatedSettings
        }
    }

    override fun convertToString(value: Merchant): String = value.apiValue

    override fun convertToValue(value: String): Merchant = Merchant.fromApiValue(value)
}
