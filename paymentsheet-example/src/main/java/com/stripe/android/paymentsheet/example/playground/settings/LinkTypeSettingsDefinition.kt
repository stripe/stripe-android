package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object LinkTypeSettingsDefinition :
    PlaygroundSettingDefinition<LinkType>,
    PlaygroundSettingDefinition.Saveable<LinkType> by EnumSaveable(
        key = "LinkType",
        values = LinkType.entries.toTypedArray(),
        defaultValue = LinkType.Web
    ),
    PlaygroundSettingDefinition.Displayable<LinkType> {
    override val displayName: String = "Link Type"

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ): List<PlaygroundSettingDefinition.Displayable.Option<LinkType>> {
        return LinkType.entries.map { linkType ->
            option(linkType.value, linkType)
        }
    }

    override fun configure(
        value: LinkType,
        checkoutRequestBuilder: CheckoutRequest.Builder,
    ) {
        when (value) {
            LinkType.Native -> {
//                FeatureFlags.suppressNativeLink.setEnabled(false)
                checkoutRequestBuilder.useLink(true)
                checkoutRequestBuilder.linkMode("native")
            }
            LinkType.NativeAttest -> {
//                FeatureFlags.suppressNativeLink.setEnabled(false)
                checkoutRequestBuilder.useLink(true)
                checkoutRequestBuilder.linkMode("attest")
            }
            LinkType.Web -> {
//                FeatureFlags.suppressNativeLink.setEnabled(true)
                checkoutRequestBuilder.useLink(true)
                checkoutRequestBuilder.linkMode("web")
            }
            LinkType.Off -> {
                checkoutRequestBuilder.useLink(false)
                checkoutRequestBuilder.linkMode(null)
            }
            LinkType.Test -> {
                checkoutRequestBuilder.useLink(true)
                checkoutRequestBuilder.linkMode(null)
            }
        }
    }
}

enum class LinkType(override val value: String) : ValueEnum {
    Native("Native"),
    NativeAttest("Native + Attest"),
    Web("Web"),
    Off("Off"),
    Test("Test"),
}
