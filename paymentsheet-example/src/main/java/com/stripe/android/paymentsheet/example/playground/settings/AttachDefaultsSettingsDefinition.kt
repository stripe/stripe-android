package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object AttachDefaultsSettingsDefinition : BooleanSettingsDefinition(
    key = "attachDefaults",
    displayName = "Attach Defaults",
    defaultValue = true,
) {
    override fun configure(
        value: Boolean,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState,
        configurationData: PaymentSheetConfigurationData,
    ) {
        configurationData.updateBillingDetails { copy(attachDefaultsToPaymentMethod = value) }
    }
}
