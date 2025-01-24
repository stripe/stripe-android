package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.utils.FeatureFlags

internal object LinkTypeSettingsDefinition :
    PlaygroundSettingDefinition<LinkType>,
    PlaygroundSettingDefinition.Saveable<LinkType> by EnumSaveable(
        key = "LinkType",
        values = LinkType.entries.toTypedArray(),
        defaultValue = LinkType.Native
    ),
    PlaygroundSettingDefinition.Displayable<LinkType> {
    override val displayName: String = "Link Type"

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ): List<PlaygroundSettingDefinition.Displayable.Option<LinkType>> {
        return listOf(
            option("Native", LinkType.Native),
            option("Native + Attest", LinkType.NativeAttest),
            option("Web", LinkType.Web),
        )
    }

    override fun setValue(value: LinkType) {
        when (value) {
            LinkType.Native -> {
                FeatureFlags.nativeLinkEnabled.setEnabled(true)
                FeatureFlags.nativeLinkAttestationEnabled.setEnabled(false)
            }
            LinkType.NativeAttest -> {
                FeatureFlags.nativeLinkEnabled.setEnabled(true)
                FeatureFlags.nativeLinkAttestationEnabled.setEnabled(true)
            }
            LinkType.Web -> {
                FeatureFlags.nativeLinkEnabled.setEnabled(false)
                FeatureFlags.nativeLinkAttestationEnabled.setEnabled(false)
            }
        }
    }
}

enum class LinkType(override val value: String) : ValueEnum {
    Native("Native"),
    NativeAttest("Native + Attest"),
    Web("Web"),
}
