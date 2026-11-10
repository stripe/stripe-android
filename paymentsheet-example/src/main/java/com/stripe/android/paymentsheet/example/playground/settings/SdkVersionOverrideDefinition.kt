package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.networking.RequestHeadersFactory

/** Reports another SDK version to Stripe, for testing server behavior that depends on it. */
internal object SdkVersionOverrideDefinition :
    PlaygroundSettingDefinition<String>,
    PlaygroundSettingDefinition.Saveable<String>,
    PlaygroundSettingDefinition.Displayable<String> {
    override val key: String = "sdkVersionOverride"
    override val displayName: String = "SDK version override"
    override val defaultValue: String = ""

    override fun convertToString(value: String): String = value
    override fun convertToValue(value: String): String = value

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = emptyList<PlaygroundSettingDefinition.Displayable.Option<String>>()

    override fun setValue(value: String) {
        RequestHeadersFactory.SDK_VERSION_NAME_OVERRIDE = value.trim().takeIf { it.isNotEmpty() }
    }
}
