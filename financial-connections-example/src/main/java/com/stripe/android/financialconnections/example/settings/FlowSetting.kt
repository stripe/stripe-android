package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.Flow
import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody

data class FlowSetting(
    override val selectedOption: Flow = Flow.Data,
    override val key: String = "flow",
) : Saveable<Flow>, SingleChoiceSetting<Flow>(
    displayName = "Flow",
    options = Flow.values().map { Option(it.name, it) },
    selectedOption = selectedOption
) {
    override fun lasRequest(body: LinkAccountSessionBody): LinkAccountSessionBody = body

    override fun paymentIntentRequest(body: PaymentIntentBody): PaymentIntentBody = body

    override fun valueUpdated(currentSettings: List<Setting<*>>, value: Flow): List<Setting<*>> =
        replace(currentSettings, this.copy(selectedOption = value))

    override fun convertToString(value: Flow): String {
        return value.apiValue
    }

    override fun convertToValue(value: String): Flow {
        return Flow.fromApiValue(value)
    }
}
