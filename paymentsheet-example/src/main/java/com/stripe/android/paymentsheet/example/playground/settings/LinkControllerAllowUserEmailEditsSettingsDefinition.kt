package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.link.LinkController
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object LinkControllerAllowUserEmailEditsSettingsDefinition : BooleanSettingsDefinition(
    key = "linkControllerAllowUserEmailEdits",
    displayName = "LinkController: allow user to edit email",
    defaultValue = true,
) {
    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType == PlaygroundConfigurationData.IntegrationType.LinkController
    }

    override fun configure(
        value: Boolean,
        configurationBuilder: LinkController.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.LinkControllerConfigurationData
    ) {
        configurationBuilder.allowUserEmailEdits(value)
    }
}
