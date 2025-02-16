package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object LinkTypeSettingsDefinition :
    PlaygroundSettingDefinition<LinkMode>,
    PlaygroundSettingDefinition.Saveable<LinkMode> by EnumSaveable(
        key = "LinkMode",
        values = LinkMode.entries.toTypedArray(),
        defaultValue = LinkMode.Test
    ),
    PlaygroundSettingDefinition.Displayable<LinkMode> {
    override val displayName: String = "Link Mode"

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ): List<PlaygroundSettingDefinition.Displayable.Option<LinkMode>> {
        return LinkMode.entries.map { linkType ->
            option(linkType.value, linkType)
        }
    }

    override fun configure(
        value: LinkMode,
        checkoutRequestBuilder: CheckoutRequest.Builder,
    ) {
        when (value) {
            LinkMode.Native -> {
                checkoutRequestBuilder.useLink(true)
                checkoutRequestBuilder.linkMode("native")
            }
            LinkMode.NativeAttest -> {
                checkoutRequestBuilder.useLink(true)
                checkoutRequestBuilder.linkMode("attest")
            }
            LinkMode.Web -> {
                checkoutRequestBuilder.useLink(true)
                checkoutRequestBuilder.linkMode("web")
            }
            LinkMode.Off -> {
                checkoutRequestBuilder.useLink(false)
                checkoutRequestBuilder.linkMode(null)
            }
            LinkMode.Test -> {
                FeatureFlags.suppressNativeLink.setEnabled(true)
                checkoutRequestBuilder.useLink(true)
                checkoutRequestBuilder.linkMode(null)
            }
        }
    }
}

enum class LinkMode(override val value: String) : ValueEnum {
    Test("Test"),
    Native("Native"),
    NativeAttest("Native + Attest"),
    Web("Web"),
    Off("Off"),
}
