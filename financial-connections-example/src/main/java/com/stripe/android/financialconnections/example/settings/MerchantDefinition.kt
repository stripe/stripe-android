package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.Merchant
import com.stripe.android.financialconnections.example.data.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.PaymentIntentBody
import com.stripe.android.financialconnections.example.settings.PlaygroundSettingDefinition.Displayable.Option

internal object MerchantDefinition : PlaygroundSettingDefinition.Displayable<Merchant>,
    PlaygroundSettingDefinition.Saveable<Merchant> {
    override val displayName: String
        get() = "Merchant"
    override val options: List<Option<Merchant>>
        get() = Merchant.values().map { option(it.name, it) }

    override fun lasRequest(
        body: LinkAccountSessionBody,
        value: Merchant?
    ): LinkAccountSessionBody = body.copy(
        flow = value?.apiValue
    )

    override fun paymentIntentRequest(
        body: PaymentIntentBody,
        value: Merchant?
    ) = body.copy(
        flow = value?.apiValue
    )

    override fun valueUpdated(
        value: Merchant,
        playgroundSettings: PlaygroundSettings
    ): PlaygroundSettings {
        return if (value == Merchant.Other) {
            playgroundSettings
                .withValue(PublicKeyDefinition, "")
                .withValue(PrivateKeyDefinition, "")
        } else {
            playgroundSettings
                .remove(PublicKeyDefinition)
                .remove(PrivateKeyDefinition)
        }
    }

    override val key: String = "merchant"
    override val defaultValue: Merchant = Merchant.Test

    override fun convertToValue(value: String) = Merchant.valueOf(value)

    override fun convertToString(value: Merchant): String = value.name
}