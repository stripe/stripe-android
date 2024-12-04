package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object InitializationTypeSettingsDefinition :
    PlaygroundSettingDefinition<InitializationType>,
    PlaygroundSettingDefinition.Saveable<InitializationType> by EnumSaveable(
        key = "initialization",
        values = InitializationType.entries.toTypedArray(),
        defaultValue = InitializationType.Normal,
    ),
    PlaygroundSettingDefinition.Displayable<InitializationType> {
    override val displayName: String = "Initialization"

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = buildList<PlaygroundSettingDefinition.Displayable.Option<InitializationType>> {
        if (configurationData.integrationType != PlaygroundConfigurationData.IntegrationType.Embedded) {
            add(option("Normal", InitializationType.Normal))
        }
        add(option("Deferred CSC", InitializationType.DeferredClientSideConfirmation))
        add(option("Deferred SSC", InitializationType.DeferredServerSideConfirmation))
        add(option("Deferred SSC + MC", InitializationType.DeferredManualConfirmation))
        add(option("Deferred SSC + MP", InitializationType.DeferredMultiprocessor))
    }

    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType.isPaymentFlow()
    }

    override fun configure(
        value: InitializationType,
        checkoutRequestBuilder: CheckoutRequest.Builder
    ) {
        checkoutRequestBuilder.initialization(value.value)
    }
}

enum class InitializationType(override val value: String) : ValueEnum {
    Normal("Normal"),
    DeferredClientSideConfirmation("Deferred CSC"),
    DeferredServerSideConfirmation("Deferred SSC"),
    DeferredManualConfirmation("Deferred SSC + MC"),
    DeferredMultiprocessor("Deferred SSC + MP"),
}
