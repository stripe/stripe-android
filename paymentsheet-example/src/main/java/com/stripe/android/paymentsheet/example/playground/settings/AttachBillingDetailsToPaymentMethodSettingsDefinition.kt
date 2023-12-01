package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object AttachBillingDetailsToPaymentMethodSettingsDefinition : BooleanSettingsDefinition(
    key = "attachDefaults",
    displayName = "Attach Billing Details to Payment Method",
    defaultValue = true,
) {
    override fun configure(
        value: Boolean,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData,
    ) {
        configurationData.updateBillingDetails { copy(attachDefaultsToPaymentMethod = value) }
    }
}
