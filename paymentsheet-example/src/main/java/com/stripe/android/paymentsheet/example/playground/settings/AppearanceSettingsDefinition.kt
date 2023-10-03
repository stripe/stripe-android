package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import com.stripe.android.paymentsheet.example.playground.activity.AppearanceStore

internal object AppearanceSettingsDefinition : PlaygroundSettingDefinition<Unit>(
    key = "appearance",
    displayName = "", // Not displayed.
) {
    override val defaultValue: Unit = Unit
    override val options: List<Option<Unit>> = emptyList()
    override val saveToSharedPreferences: Boolean = false

    override fun convertToValue(value: String) {
        // No value, we're saving in memory only.
    }

    override fun convertToString(value: Unit): String {
        return ""
    }

    override fun configure(
        value: Unit,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState,
        configurationData: PaymentSheetConfigurationData
    ) {
        configurationBuilder.appearance(AppearanceStore.state)
    }
}
