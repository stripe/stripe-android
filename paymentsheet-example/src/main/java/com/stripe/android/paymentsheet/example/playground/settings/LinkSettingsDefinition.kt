@file:OptIn(LinkHiddenWalletButtonPreview::class)

package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.LinkHiddenWalletButtonPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object LinkSettingsDefinition :
    PlaygroundSettingDefinition<LinkDisplaySetting>,
    PlaygroundSettingDefinition.Saveable<LinkDisplaySetting> by EnumSaveable(
        key = "link",
        values = LinkDisplaySetting.entries.toTypedArray(),
        defaultValue = LinkDisplaySetting.Automatic,
    ),
    PlaygroundSettingDefinition.Displayable<LinkDisplaySetting> {
    override val displayName: String = "Link"

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ): List<PlaygroundSettingDefinition.Displayable.Option<LinkDisplaySetting>> {
        return LinkDisplaySetting.entries.map { setting -> option(setting.value, setting) }
    }

    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        return configurationData.integrationType.isPaymentFlow()
    }

    override fun configure(
        value: LinkDisplaySetting,
        checkoutRequestBuilder: CheckoutRequest.Builder,
    ) {
        checkoutRequestBuilder.useLink(value.useLink)
    }

    override fun configure(
        value: LinkDisplaySetting,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        configurationBuilder.link(
            link = makeLinkConfiguration(value),
        )
    }

    override fun configure(
        value: LinkDisplaySetting,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        configurationBuilder.link(
            link = makeLinkConfiguration(value),
        )
    }

    private fun makeLinkConfiguration(value: LinkDisplaySetting): PaymentSheet.LinkConfiguration {
        return PaymentSheet.LinkConfiguration(
            display = value.display,
        )
    }
}

@OptIn(LinkHiddenWalletButtonPreview::class)
internal enum class LinkDisplaySetting(
    override val value: String,
    val useLink: Boolean,
    val display: PaymentSheet.LinkConfiguration.Display,
) : ValueEnum {
    Automatic("automatic", true, PaymentSheet.LinkConfiguration.Display.Automatic),
    Never("never", false, PaymentSheet.LinkConfiguration.Display.Never),
    Hidden("hidden", true, PaymentSheet.LinkConfiguration.Display.Hidden),
}
