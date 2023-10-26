package com.stripe.android.financialconnections.example.settings

import com.stripe.android.financialconnections.example.NativeOverride
import com.stripe.android.financialconnections.example.data.model.LinkAccountSessionBody
import com.stripe.android.financialconnections.example.data.model.PaymentIntentBody
import com.stripe.android.financialconnections.example.settings.PlaygroundSettingDefinition.Option

internal object NativeOverrideDefinition : PlaygroundSettingDefinition<NativeOverride> {

    override val displayName: String = "Native Override"
    override val options: List<Option<NativeOverride>>
        get() = listOf(
            option("None", NativeOverride.None),
            option("Native", NativeOverride.Native),
            option("Web", NativeOverride.Web),
        )

    override fun lasRequest(
        body: LinkAccountSessionBody,
        value: NativeOverride
    ): LinkAccountSessionBody = body

    override fun paymentIntentRequest(
        body: PaymentIntentBody,
        value: NativeOverride
    ): PaymentIntentBody = body

    override val key: String = "financial_connections_override_native"
    override val defaultValue: NativeOverride = NativeOverride.None

    override fun convertToValue(value: String): NativeOverride = NativeOverride.fromApiValue(value)

    override fun convertToString(value: NativeOverride): String = value.apiValue
}
