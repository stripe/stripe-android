package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.FeatureState

/**
 * Setting to control whether the checkout session is created with
 * `saved_payment_method_options.payment_method_save: enabled`.
 *
 * When enabled, customers can choose to save their payment method for future use.
 * The SDK can then pass `save_payment_method=true/false` during confirmation.
 */
internal object CheckoutSessionSaveSettingsDefinition : BooleanSettingsDefinition(
    defaultValue = true,
    displayName = "Checkout Session Save",
    key = "checkout_session_payment_method_save"
) {
    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        if (!configurationData.integrationType.isPaymentFlow()) {
            return false
        }
        // Only applicable when using Checkout Session initialization
        return settings[InitializationTypeSettingsDefinition] == InitializationType.CheckoutSession
    }

    override fun createOptions(
        configurationData: PlaygroundConfigurationData
    ) = listOf(
        PlaygroundSettingDefinition.Displayable.Option("Enabled", true),
        PlaygroundSettingDefinition.Displayable.Option("Disabled", false),
    )

    override fun configure(value: Boolean, checkoutRequestBuilder: CheckoutRequest.Builder) {
        if (value) {
            checkoutRequestBuilder.checkoutSessionPaymentMethodSave(FeatureState.Enabled)
        } else {
            checkoutRequestBuilder.checkoutSessionPaymentMethodSave(FeatureState.Disabled)
        }
    }
}
