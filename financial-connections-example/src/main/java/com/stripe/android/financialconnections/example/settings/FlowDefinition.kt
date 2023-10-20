package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.Flow
import com.stripe.android.financialconnections.example.data.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.PaymentIntentBody
import com.stripe.android.financialconnections.example.settings.PlaygroundSettingDefinition.Displayable.Option

internal object FlowDefinition :
    PlaygroundSettingDefinition.Displayable<Flow>,
    PlaygroundSettingDefinition.Saveable<Flow> {
    override val displayName: String = "Flow"
    override val options: List<Option<Flow>> = Flow.values().map { option(it.name, it) }

    override fun lasRequest(
        body: LinkAccountSessionBody,
        value: Flow?
    ): LinkAccountSessionBody = body

    override fun paymentIntentRequest(
        body: PaymentIntentBody,
        value: Flow?
    ) = body

    override val key: String = "flow"
    override val defaultValue: Flow
        get() = Flow.PaymentIntent

    override fun convertToValue(value: String): Flow = Flow.fromApiValue(value)

    override fun convertToString(value: Flow): String = value.apiValue
}
