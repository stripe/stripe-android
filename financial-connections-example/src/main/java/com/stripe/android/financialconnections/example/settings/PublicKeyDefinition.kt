package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody
import com.stripe.android.financialconnections.example.settings.PlaygroundSettingDefinition.Option

internal object PublicKeyDefinition : PlaygroundSettingDefinition.Saveable<String> {
    override val displayName: String = "Public key"
    override val options: List<Option<String>> = emptyList()

    override fun lasRequest(
        body: LinkAccountSessionBody,
        value: String
    ): LinkAccountSessionBody = body.copy(
        publishableKey = value
    )

    override fun paymentIntentRequest(
        body: PaymentIntentBody,
        value: String
    ) = body.copy(
        publishableKey = value
    )

    override val key: String = "public_key"
    override val defaultValue: String = ""

    override fun convertToValue(value: String): String = value

    override fun convertToString(value: String): String = value
}
