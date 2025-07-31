package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.elements.payment.LinkConfiguration
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object LinkSettingsDefinition : BooleanSettingsDefinition(
    key = "link",
    displayName = "Link",
    defaultValue = true,
) {
    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType.isPaymentFlow()
    }

    override fun configure(
        value: Boolean,
        checkoutRequestBuilder: CheckoutRequest.Builder,
    ) {
        checkoutRequestBuilder.useLink(value)
    }

    override fun configure(
        value: Boolean,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        configurationBuilder.link(
            link = makeLinkConfiguration(value),
        )
    }

    override fun configure(
        value: Boolean,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        configurationBuilder.link(
            link = makeLinkConfiguration(value),
        )
    }

    private fun makeLinkConfiguration(value: Boolean): LinkConfiguration {
        return LinkConfiguration(
            display = if (value) {
                LinkConfiguration.Display.Automatic
            } else {
                LinkConfiguration.Display.Never
            }
        )
    }
}
