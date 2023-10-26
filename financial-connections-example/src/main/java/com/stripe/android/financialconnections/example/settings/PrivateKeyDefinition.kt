package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody
import com.stripe.android.financialconnections.example.settings.PlaygroundSettingDefinition.Option

internal object PrivateKeyDefinition : PlaygroundSettingDefinition<String> {
    override val displayName: String = "Secret Key"
    override val options: List<Option<String>> = emptyList()

    override fun lasRequest(
        body: LinkAccountSessionBody,
        value: String
    ): LinkAccountSessionBody = body.copy(
        secretKey = value,
    )

    override fun paymentIntentRequest(
        body: PaymentIntentBody,
        value: String
    ) = body.copy(
        secretKey = value,
    )

    override val key: String = "sk"
    override val defaultValue: String = ""

    override fun convertToValue(value: String): String = value

    override fun convertToString(value: String): String = value
}
