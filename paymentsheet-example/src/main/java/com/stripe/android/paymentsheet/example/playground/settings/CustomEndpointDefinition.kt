package com.stripe.android.paymentsheet.example.playground.settings

internal object CustomEndpointDefinition :
    PlaygroundSettingDefinition<String?>,
    PlaygroundSettingDefinition.Saveable<String?> {

    override val key: String = "custom-endpoint"
    override val defaultValue: String? = null

    override fun convertToValue(value: String): String? = value.takeUnless { it.isBlank() }
    override fun convertToString(value: String?): String = value.orEmpty()
}
