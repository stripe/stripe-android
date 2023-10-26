package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody
import com.stripe.android.financialconnections.example.settings.PlaygroundSettingDefinition.Option

internal object EmailDefinition : PlaygroundSettingDefinition<String> {
    override val defaultValue: String = ""
    override val displayName: String = "Customer email"
    override val options: List<Option<String>> = emptyList()

    override fun lasRequest(
        body: LinkAccountSessionBody,
        value: String
    ): LinkAccountSessionBody = body.copy(
        customerEmail = value
    )

    override fun paymentIntentRequest(
        body: PaymentIntentBody,
        value: String
    ) = body.copy(
        customerEmail = value
    )

}
