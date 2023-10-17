package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.Flow
import com.stripe.android.financialconnections.example.data.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.PaymentIntentBody
import com.stripe.android.financialconnections.example.settings.PlaygroundSettingDefinition.Displayable.Option

internal class FlowDefinition : PlaygroundSettingDefinition.Displayable<Flow> {
    override val displayName: String
        get() = "Flow"
    override val options: List<Option<Flow>>
        get() = Flow.values().map { option(it.name, it) }

    override fun lasRequest(
        body: LinkAccountSessionBody,
        value: Any?
    ): LinkAccountSessionBody = body

    override fun paymentIntentRequest(
        body: PaymentIntentBody,
        value: Any?
    ) = body
}