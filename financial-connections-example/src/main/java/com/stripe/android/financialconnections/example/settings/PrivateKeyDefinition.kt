package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.data.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.PaymentIntentBody
import com.stripe.android.financialconnections.example.settings.PlaygroundSettingDefinition.Displayable.Option

internal object PrivateKeyDefinition : PlaygroundSettingDefinition.Displayable<String>,
    PlaygroundSettingDefinition.Saveable<String> {
    override val displayName: String = "Secret Key"
    override val options: List<Option<String>> = emptyList()

    override fun lasRequest(
        body: LinkAccountSessionBody,
        value: Any?
    ): LinkAccountSessionBody = body.copy(
        secretKey = value as String,
    )

    override fun paymentIntentRequest(
        body: PaymentIntentBody,
        value: Any?
    ) = body.copy(
        secretKey = value as String,
    )

    override val key: String = "sk"
    override val defaultValue: String = ""

    override fun convertToValue(value: String): String = value

    override fun convertToString(value: String): String = value
}