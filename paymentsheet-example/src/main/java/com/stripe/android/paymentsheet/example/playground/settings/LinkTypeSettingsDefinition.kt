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
        return listOf(
            option("Default", LinkType.Default),
            option("Native", LinkType.Native),
            option("Native + Attest", LinkType.NativeAttest),
            option("Web", LinkType.Web),
        )
    }

    override fun configure(
        value: LinkType,
        checkoutRequestBuilder: CheckoutRequest.Builder,
    ) {
        when (value) {
            LinkType.Native -> {
                checkoutRequestBuilder.linkMode("native")
            }
            LinkType.NativeAttest -> {
                checkoutRequestBuilder.linkMode("attest")
            }
            LinkType.Web -> {
                checkoutRequestBuilder.linkMode("web")
            }
            LinkType.Default -> {
                checkoutRequestBuilder.linkMode(null)
            }
        }
    }
}

enum class LinkType(override val value: String) : ValueEnum {
    Native("Native"),
    NativeAttest("Native + Attest"),
    Web("Web"),
    Default("Default"),
}
