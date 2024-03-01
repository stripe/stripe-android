package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

internal object PaymentMethodOrderSettingsDefinition :
    PlaygroundSettingDefinition<String>,
    PlaygroundSettingDefinition.Saveable<String>,
    PlaygroundSettingDefinition.Displayable<String> {
    override val key: String = "paymentMethodOrder"
    override val displayName: String = "Payment method order"
    override val defaultValue: String = ""
    override val options: List<PlaygroundSettingDefinition.Displayable.Option<String>> = emptyList()

    override fun convertToString(value: String): String = value
    override fun convertToValue(value: String): String = value

    override fun configure(
        value: String,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        if (value.isNotEmpty()) {
            configurationBuilder.paymentMethodOrder(value.split(",").map { it.trim() })
        }
    }
}
