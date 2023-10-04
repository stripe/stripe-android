package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object SupportedPaymentMethodsSettingsDefinition :
    PlaygroundSettingDefinition<List<String>?>,
    PlaygroundSettingDefinition.Saveable<List<String>?> {
    override fun configure(value: List<String>?, checkoutRequestBuilder: CheckoutRequest.Builder) {
        if (!value.isNullOrEmpty()) {
            checkoutRequestBuilder.supportedPaymentMethods(value)
        }
    }

    override val key: String = "supportedPaymentMethods"
    override val defaultValue: List<String> = emptyList()
    override val saveToSharedPreferences: Boolean = false

    override fun convertToValue(value: String): List<String> {
        return value.split(",").filter { it.isNotEmpty() }
    }

    override fun convertToString(value: List<String>?): String {
        return value.orEmpty().joinToString(separator = ",")
    }
}
