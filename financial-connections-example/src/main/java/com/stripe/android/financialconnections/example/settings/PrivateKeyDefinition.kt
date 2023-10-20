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
        value: String?
    ): LinkAccountSessionBody = body.copy(
        secretKey = value,
    )

    override fun paymentIntentRequest(
        body: PaymentIntentBody,
        value: String?
    ) = body.copy(
        secretKey = value,
    )

    override val key: String = "sk"
    override val defaultValue: String = ""

    override fun convertToValue(value: String): String = value

    override fun convertToString(value: String): String = value
}