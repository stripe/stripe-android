package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.NativeOverride
import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

data class NativeSetting(
    override val selectedOption: NativeOverride = NativeOverride.None,
    override val key: String = "financial_connections_override_native",
) : Saveable<NativeOverride>, SingleChoiceSetting<NativeOverride>(
    displayName = "Merchant",
    options = listOf(
        Option("None", NativeOverride.None),
        Option("Native", NativeOverride.Native),
        Option("Web", NativeOverride.Web),
    ),
    selectedOption = selectedOption
) {
    override fun lasRequest(
        body: LinkAccountSessionBody,
    ): LinkAccountSessionBody = body

    override fun paymentIntentRequest(
        body: PaymentIntentBody,
    ): PaymentIntentBody = body

    override fun valueUpdated(currentSettings: List<Setting<*>>, value: NativeOverride): List<Setting<*>> {
        return replace(currentSettings, this.copy(selectedOption = value))
    }

    override fun convertToValue(value: String): NativeOverride = NativeOverride.fromApiValue(value)

    override fun convertToString(value: NativeOverride): String = value.apiValue
}
