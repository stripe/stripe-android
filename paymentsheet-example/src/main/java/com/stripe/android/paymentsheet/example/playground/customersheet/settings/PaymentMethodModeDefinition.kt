package com.stripe.android.paymentsheet.example.playground.customersheet.settings

internal object PaymentMethodModeDefinition :
    CustomerSheetPlaygroundSettingDefinition<PaymentMethodMode>,
    CustomerSheetPlaygroundSettingDefinition.Saveable<PaymentMethodMode> by EnumSaveable(
        key = "paymentMethodMode",
        values = PaymentMethodMode.entries.toTypedArray(),
        defaultValue = PaymentMethodMode.SetupIntent,
    ),
    CustomerSheetPlaygroundSettingDefinition.Displayable<PaymentMethodMode> {
    override val displayName: String = "Payment Method Mode"

    override val options by lazy {
        listOf(
            option("Setup Intent", PaymentMethodMode.SetupIntent),
            option("Create And Attach", PaymentMethodMode.CreateAndAttach),
        )
    }
}

enum class PaymentMethodMode(override val value: String) : ValueEnum {
    SetupIntent("setup_intent"),
    CreateAndAttach("create_and_attach")
}
