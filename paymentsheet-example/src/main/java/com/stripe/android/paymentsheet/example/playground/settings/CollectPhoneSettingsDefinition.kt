package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object CollectPhoneSettingsDefinition : CollectionModeSettingsDefinition(
    key = "collectPhone",
    displayName = "Collect Phone",
) {
    override fun configure(
        value: CollectionMode,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData,
    ) {
        configurationData.updateBillingDetails { copy(phone = value) }
    }
}
