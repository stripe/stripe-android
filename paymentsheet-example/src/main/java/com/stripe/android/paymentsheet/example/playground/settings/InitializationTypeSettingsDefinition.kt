package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest

internal object InitializationTypeSettingsDefinition :
    PlaygroundSettingDefinition<InitializationTypeSettingsDefinition.InitializationType>(
        key = "initialization",
        displayName = "Initialization",
    ) {
    override val defaultValue: InitializationType = InitializationType.Normal
    override val options: List<Option<InitializationType>> = listOf(
        Option("Normal", InitializationType.Normal),
        Option("Deferred CSC", InitializationType.DeferredClientSideConfirmation),
        Option("Deferred SSC", InitializationType.DeferredServerSideConfirmation),
        Option("Deferred SSC + MC", InitializationType.DeferredManualConfirmation),
        Option("Deferred SSC + MP", InitializationType.DeferredMultiprocessor),
    )

    override fun convertToValue(value: String): InitializationType {
        return InitializationType.values().firstOrNull { it.value == value } ?: defaultValue
    }

    override fun convertToString(value: InitializationType): String {
        return value.value
    }

    override fun configure(
        value: InitializationType,
        checkoutRequestBuilder: CheckoutRequest.Builder
    ) {
        checkoutRequestBuilder.initialization(value.value)
    }

    enum class InitializationType(val value: String) {
        Normal("Normal"),
        DeferredClientSideConfirmation("Deferred CSC"),
        DeferredServerSideConfirmation("Deferred SSC"),
        DeferredManualConfirmation("Deferred SSC + MC"),
        DeferredMultiprocessor("Deferred SSC + MP"),
    }
}
