package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.Merchant
import com.stripe.android.financialconnections.example.data.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.PaymentIntentBody
import com.stripe.android.financialconnections.example.settings.PlaygroundSettingDefinition.Displayable.Option

internal class MerchantDefinition : PlaygroundSettingDefinition<Merchant>,
    PlaygroundSettingDefinition.Displayable<Merchant> {
    override val displayName: String
        get() = "Merchant"
    override val options: List<Option<Merchant>>
        get() = Merchant.values().map { option(it.name, it) }

    override fun lasRequest(
        body: LinkAccountSessionBody,
        value: Any?
    ): LinkAccountSessionBody = body.copy(
        flow = (value as Merchant?)?.flow
    )

    override fun paymentIntentRequest(
        body: PaymentIntentBody,
        value: Any?
    ) = body.copy(
        flow = (value as Merchant?)?.flow
    )
}