package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.core.utils.FeatureFlags

internal object EditSavedCardPaymentMethodDefinition : BooleanSettingsDefinition(
    key = "edit_saved_card_payment_method",
    displayName = "Edit Saved Card Payment Method",
    defaultValue = false
) {
    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ): List<PlaygroundSettingDefinition.Displayable.Option<Boolean>> {
        return listOf(
            option("On", true),
            option("Off", false)
        )
    }

    override fun setValue(value: Boolean) {
        FeatureFlags.editSavedCardPaymentMethodEnabled.setEnabled(value)
    }
}
