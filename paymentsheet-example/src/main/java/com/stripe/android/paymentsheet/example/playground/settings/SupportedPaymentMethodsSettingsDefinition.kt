package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object SupportedPaymentMethodsSettingsDefinition :
    PlaygroundSettingDefinition<List<String>?> {
    override fun configure(value: List<String>?, checkoutRequestBuilder: CheckoutRequest.Builder) {
        if (!value.isNullOrEmpty()) {
            checkoutRequestBuilder.supportedPaymentMethods(value)
        }
    }
}
