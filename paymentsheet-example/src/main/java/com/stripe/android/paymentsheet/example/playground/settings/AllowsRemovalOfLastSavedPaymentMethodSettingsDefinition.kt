package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object AllowsRemovalOfLastSavedPaymentMethodSettingsDefinition : BooleanSettingsDefinition(
    key = "allowsRemovalOfLastSavedPaymentMethod",
    displayName = "Allows removal of last saved payment method",
    defaultValue = true,
) {
    @OptIn(ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi::class)
    override fun configure(
        value: Boolean,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        configurationBuilder.allowsRemovalOfLastSavedPaymentMethod(value)
    }
}
