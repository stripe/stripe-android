package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.utils.FeatureFlags

/**
 * Overrides the server's flags for inline Link verification in the Embedded Payment Element. Off by default, so other
 * tests never send a verification code automatically.
 */
internal object EmbeddedLinkInlineOtpSettingsDefinition :
    PlaygroundSettingDefinition<EmbeddedLinkInlineOtpSettingsDefinition.InlineOtp>,
    PlaygroundSettingDefinition.Saveable<EmbeddedLinkInlineOtpSettingsDefinition.InlineOtp> by EnumSaveable(
        key = "embeddedLinkInlineOtp",
        values = InlineOtp.entries.toTypedArray(),
        defaultValue = InlineOtp.Off,
    ),
    PlaygroundSettingDefinition.Displayable<EmbeddedLinkInlineOtpSettingsDefinition.InlineOtp> {
    override val displayName: String = "Embedded inline Link OTP"

    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        return configurationData.integrationType == PlaygroundConfigurationData.IntegrationType.Embedded &&
            settings[LinkSettingsDefinition] != LinkDisplaySetting.Never
    }

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ): List<PlaygroundSettingDefinition.Displayable.Option<InlineOtp>> {
        return listOf(
            option("Off", InlineOtp.Off),
            option("On", InlineOtp.On),
            option("Force dialog", InlineOtp.ForceDialog),
            option("Server", InlineOtp.Server),
        )
    }

    override fun setValue(value: InlineOtp) {
        when (value) {
            InlineOtp.Off -> {
                FeatureFlags.embeddedInlineLinkOtp.setEnabled(false)
                FeatureFlags.embeddedInlineLinkOtpForceDialog.setEnabled(false)
            }
            InlineOtp.On -> {
                FeatureFlags.embeddedInlineLinkOtp.setEnabled(true)
                FeatureFlags.embeddedInlineLinkOtpForceDialog.setEnabled(false)
            }
            InlineOtp.ForceDialog -> {
                FeatureFlags.embeddedInlineLinkOtp.setEnabled(true)
                FeatureFlags.embeddedInlineLinkOtpForceDialog.setEnabled(true)
            }
            InlineOtp.Server -> {
                FeatureFlags.embeddedInlineLinkOtp.reset()
                FeatureFlags.embeddedInlineLinkOtpForceDialog.reset()
            }
        }
    }

    enum class InlineOtp(override val value: String) : ValueEnum {
        Off("off"),
        On("on"),
        ForceDialog("forceDialog"),
        Server("server"),
    }
}
