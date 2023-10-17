package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.data.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.PaymentIntentBody
import com.stripe.android.financialconnections.example.settings.PlaygroundSettingDefinition.Displayable.Option

internal object PublicKeyDefinition : PlaygroundSettingDefinition.Displayable<String>,
    PlaygroundSettingDefinition.Saveable<String> {
    override val displayName: String = "Public key"
    override val options: List<Option<String>> = emptyList()

    override fun lasRequest(
        body: LinkAccountSessionBody,
        value: Any?
    ): LinkAccountSessionBody = body.copy(
        publishableKey = value as String
    )

    override fun paymentIntentRequest(
        body: PaymentIntentBody,
        value: Any?
    ) = body.copy(
        publishableKey = value as String
    )

    override val key: String = "public_key"
    override val defaultValue: String = ""

    override fun convertToValue(value: String): String = value

    override fun convertToString(value: String): String = value
}