package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object SupportedPaymentMethodsSettingsDefinition :
    PlaygroundSettingDefinition<List<String>?>(
        key = "supportedPaymentMethods",
        displayName = "", // This is not a UI setting, only used for tests.
    ) {
    override val defaultValue: List<String>? = null

    override val options: List<Option<List<String>?>> = emptyList()

    override val saveToSharedPreferences: Boolean = false

    override fun configure(value: List<String>?, checkoutRequestBuilder: CheckoutRequest.Builder) {
        if (!value.isNullOrEmpty()) {
            checkoutRequestBuilder.supportedPaymentMethods(value)
        }
    }

    override fun convertToValue(value: String): List<String> {
        return value.split(",").filter { it.isNotEmpty() }
    }

    override fun convertToString(value: List<String>?): String {
        return value.orEmpty().joinToString(separator = ",")
    }
}
