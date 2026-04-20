package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.utils.FeatureFlags

internal object LinkBrandSettingsDefinition :
    PlaygroundSettingDefinition<LinkBrandOverride>,
    PlaygroundSettingDefinition.Saveable<LinkBrandOverride> by EnumSaveable(
        key = "LinkBrand",
        values = LinkBrandOverride.entries.toTypedArray(),
        defaultValue = LinkBrandOverride.Default
    ),
    PlaygroundSettingDefinition.Displayable<LinkBrandOverride> {
    override val displayName: String = "Link Brand"

    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        if (!configurationData.integrationType.isPaymentFlow() && !configurationData.integrationType.isSptFlow()) {
            return false
        }

        return (
            LinkSettingsDefinition.applicable(configurationData, settings) &&
                settings[LinkSettingsDefinition] == true
            )
    }

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ): List<PlaygroundSettingDefinition.Displayable.Option<LinkBrandOverride>> {
        return listOf(
            option("Default", LinkBrandOverride.Default),
            option("Link", LinkBrandOverride.Link),
            option("Notlink", LinkBrandOverride.Notlink),
        )
    }

    override fun setValue(value: LinkBrandOverride) {
        FeatureFlags.forceNotlink.setEnabled(value == LinkBrandOverride.Notlink)
    }
}

enum class LinkBrandOverride(override val value: String) : ValueEnum {
    Default("Default"),
    Link("Link"),
    Notlink("Notlink"),
}
