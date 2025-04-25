package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.playground.PlaygroundState

@OptIn(ExperimentalEmbeddedPaymentElementApi::class, ExperimentalCustomPaymentMethodsApi::class)
internal object CustomPaymentMethodsSettingDefinition :
    PlaygroundSettingDefinition<CustomPaymentMethodPlaygroundType>,
    PlaygroundSettingDefinition.Saveable<CustomPaymentMethodPlaygroundType> by EnumSaveable(
        key = "customPaymentMethods",
        values = CustomPaymentMethodPlaygroundType.entries.toTypedArray(),
        defaultValue = CustomPaymentMethodPlaygroundType.Off,
    ),
    PlaygroundSettingDefinition.Displayable<CustomPaymentMethodPlaygroundType> {
    override val displayName: String = "Custom payment methods"

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = CustomPaymentMethodPlaygroundType.entries.map {
        option(it.displayName, it)
    }

    override fun configure(
        value: CustomPaymentMethodPlaygroundType,
        configurationBuilder: EmbeddedPaymentElement.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.EmbeddedConfigurationData
    ) {
        configurationBuilder.customPaymentMethods(createCustomPaymentMethodsFromType(value))
    }

    override fun configure(
        value: CustomPaymentMethodPlaygroundType,
        configurationBuilder: PaymentSheet.Configuration.Builder,
        playgroundState: PlaygroundState.Payment,
        configurationData: PlaygroundSettingDefinition.PaymentSheetConfigurationData
    ) {
        configurationBuilder.customPaymentMethods(createCustomPaymentMethodsFromType(value))
    }

    private fun createCustomPaymentMethodsFromType(
        value: CustomPaymentMethodPlaygroundType
    ): List<PaymentSheet.CustomPaymentMethod> {
        return when (value) {
            CustomPaymentMethodPlaygroundType.On -> createCustomPaymentMethods(disableBillingDetails = true)
            CustomPaymentMethodPlaygroundType.OnWithBillingDetailsCollection ->
                createCustomPaymentMethods(disableBillingDetails = false)
            CustomPaymentMethodPlaygroundType.Off -> emptyList()
        }
    }
}

enum class CustomPaymentMethodPlaygroundType(
    val displayName: String
) : ValueEnum {
    On("On"),
    OnWithBillingDetailsCollection("On with billing details collection"),
    Off("Off");

    override val value: String
        get() = name
}

internal const val DEFAULT_CUSTOM_PAYMENT_METHOD_ID = "cpmt_1QpIMNLu5o3P18Zpwln1Sm6I"

@OptIn(ExperimentalCustomPaymentMethodsApi::class)
private fun createCustomPaymentMethods(disableBillingDetails: Boolean): List<PaymentSheet.CustomPaymentMethod> {
    return listOf(
        PaymentSheet.CustomPaymentMethod(
            id = DEFAULT_CUSTOM_PAYMENT_METHOD_ID,
            subtitle = "Pay now with BufoPay",
            disableBillingDetailCollection = disableBillingDetails,
        )
    )
}
