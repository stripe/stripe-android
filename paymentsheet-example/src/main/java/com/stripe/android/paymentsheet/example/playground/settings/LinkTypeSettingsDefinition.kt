package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.applyFeatureFlags

internal object LinkTypeSettingsDefinition :
    PlaygroundSettingDefinition<LinkType>,
    PlaygroundSettingDefinition.Saveable<LinkType> by EnumSaveable(
        key = "LinkType",
        values = LinkType.entries.toTypedArray(),
        defaultValue = LinkType.Web
    ),
    PlaygroundSettingDefinition.Displayable<LinkType> {
    override val displayName: String = "Link Type"

    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        if (!configurationData.integrationType.isPaymentFlow() && !configurationData.integrationType.isSptFlow()) {
            return false
        }

        return (
            LinkSettingsDefinition.applicable(configurationData, settings) &&
                settings[LinkSettingsDefinition] != LinkDisplaySetting.Never
            )
    }

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ): List<PlaygroundSettingDefinition.Displayable.Option<LinkType>> {
        return listOf(
            option("Server Controlled", LinkType.ServerControlled),
            option("Native", LinkType.Native),
            option("Native + Attest", LinkType.NativeAttest),
            option("Web", LinkType.Web),
        )
    }

    override fun setValue(value: LinkType) {
        value.applyFeatureFlags()
    }
}

enum class LinkType(override val value: String) : ValueEnum {
    ServerControlled("Server Controlled"),
    Native("Native"),
    NativeAttest("Native + Attest"),
    Web("Web"),
}
