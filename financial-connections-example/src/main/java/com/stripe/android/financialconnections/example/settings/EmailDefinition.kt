package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.data.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.PaymentIntentBody
import com.stripe.android.financialconnections.example.settings.PlaygroundSettingDefinition.Option

internal object EmailDefinition : PlaygroundSettingDefinition<String> {
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

    override val key: String = "customer_email"
    override val defaultValue: String = ""

    override fun convertToValue(value: String): String = value

    override fun convertToString(value: String): String = value
}
