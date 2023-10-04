package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object InitializationTypeSettingsDefinition :
    PlaygroundSettingDefinition<InitializationType>,
    PlaygroundSettingDefinition.Saveable<InitializationType> by EnumSaveable(
        key = "initialization",
        values = InitializationType.values(),
        defaultValue = InitializationType.Normal,
    ),
    PlaygroundSettingDefinition.Displayable<InitializationType> {
    override val displayName: String = "Initialization"
    override val options: List<PlaygroundSettingDefinition.Displayable.Option<InitializationType>> =
        listOf(
            option("Normal", InitializationType.Normal),
            option("Deferred CSC", InitializationType.DeferredClientSideConfirmation),
            option("Deferred SSC", InitializationType.DeferredServerSideConfirmation),
            option("Deferred SSC + MC", InitializationType.DeferredManualConfirmation),
            option("Deferred SSC + MP", InitializationType.DeferredMultiprocessor),
        )

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
