package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object MerchantOverrideDefinition : PlaygroundSettingDefinition<Pair<String, String>?> {
    override val defaultValue: Pair<String, String>? = null

    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType.isPaymentFlow()
    }

    override fun configure(value: Pair<String, String>?, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.publicKey(value?.first).privateKey(value?.second)
    }
}
