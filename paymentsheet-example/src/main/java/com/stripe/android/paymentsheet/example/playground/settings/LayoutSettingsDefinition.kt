package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState
import java.util.Locale

internal object LayoutSettingsDefinition :
    PlaygroundSettingDefinition<Layout>,
    PlaygroundSettingDefinition.Saveable<Layout> by EnumSaveable(
        key = "layout",
        values = Layout.entries.toTypedArray(),
        defaultValue = Layout.AUTOMATIC,
    ),
    PlaygroundSettingDefinition.Displayable<Layout> {
    override val displayName: String = "Payment Method Layout"

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ): List<PlaygroundSettingDefinition.Displayable.Option<Layout>> {
        return Layout.entries.map { layout ->
            option(
                layout.value.replaceFirstChar {
                    if (it.isLowerCase()) {
                        it.titlecase(Locale.getDefault())
                    } else {
                        it.toString()
                    }
                },
                layout
            )
        }
    }

    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType.isPaymentFlow()
    }

    override fun configure(
        value: Layout,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        configurationBuilder.paymentMethodLayout(value.paymentMethodLayout)
    }
}

enum class Layout(override val value: String, val paymentMethodLayout: PaymentSheet.PaymentMethodLayout) : ValueEnum {
    HORIZONTAL("horizontal", PaymentSheet.PaymentMethodLayout.Horizontal),
    VERTICAL("vertical", PaymentSheet.PaymentMethodLayout.Vertical),
    AUTOMATIC("automatic", PaymentSheet.PaymentMethodLayout.Automatic),
}
