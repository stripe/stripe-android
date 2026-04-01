package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.FeatureState

/**
 * Setting to control whether the checkout session is created with
 * `saved_payment_method_options.payment_method_remove: enabled`.
 *
 * When enabled, customers can remove saved payment methods from the checkout session.
 */
internal object CheckoutSessionRemoveSettingsDefinition : BooleanSettingsDefinition(
    defaultValue = true,
    displayName = "Checkout Session Remove",
    key = "checkout_session_payment_method_remove"
) {
    override fun applicable(
        configurationData: PlaygroundConfigurationData,
        settings: Map<PlaygroundSettingDefinition<*>, Any?>,
    ): Boolean {
        // Only applicable when using Checkout Session initialization
        return settings[InitializationTypeSettingsDefinition] == InitializationType.CheckoutSession
    }

    override fun configure(value: Boolean, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.checkoutSessionPaymentMethodRemove(FeatureState.fromBoolean(value))
    }
}
