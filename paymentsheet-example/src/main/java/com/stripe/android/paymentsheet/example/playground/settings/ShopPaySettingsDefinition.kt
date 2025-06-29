package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentelement.ShopPayPreview
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.data.ShopPayData

internal object ShopPaySettingsDefinition : BooleanSettingsDefinition(
    key = "shopPay",
    displayName = "Enable ShopPay",
    defaultValue = false
) {

    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return when (configurationData.integrationType) {
            PlaygroundConfigurationData.IntegrationType.Embedded,
            PlaygroundConfigurationData.IntegrationType.FlowController,
            PlaygroundConfigurationData.IntegrationType.FlowControllerWithSpt -> true
            PlaygroundConfigurationData.IntegrationType.PaymentSheet,
            PlaygroundConfigurationData.IntegrationType.CustomerSheet -> false
        }
    }

    override fun configure(
        value: Boolean,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        configure(value, configurationBuilder)
    }

    @OptIn(ShopPayPreview::class)
    override fun configure(
        value: Boolean,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.SharedPaymentToken,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        if (value.not()) return
        configurationBuilder.shopPayConfiguration(ShopPayData.shopPayConfiguration())
    }

    @OptIn(ShopPayPreview::class)
    private fun configure(
        value: Boolean,
        configurationBuilder: PaymentSheet.Configuration.Builder
    ) {
        if (value.not()) return
        configurationBuilder.shopPayConfiguration(ShopPayData.shopPayConfiguration())
    }
}
