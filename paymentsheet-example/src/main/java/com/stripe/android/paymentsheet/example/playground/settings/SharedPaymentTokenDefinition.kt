package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.FeatureState

internal object SharedPaymentTokenDefinition : PlaygroundSettingDefinition<Unit> {
    override val defaultValue = Unit

    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType.isSptFlow()
    }

    override fun configure(value: Unit, checkoutRequestBuilder: CheckoutRequest.Builder) {
        checkoutRequestBuilder.mode(CheckoutMode.PAYMENT.value)
        checkoutRequestBuilder.currency(Currency.USD.value)
        checkoutRequestBuilder.automaticPaymentMethods(false)
        checkoutRequestBuilder.customerKeyType(CheckoutRequest.CustomerKeyType.CustomerSession)
        checkoutRequestBuilder.paymentMethodRedisplayFeature(FeatureState.Enabled)
        checkoutRequestBuilder.paymentMethodRemoveFeature(FeatureState.Enabled)
        checkoutRequestBuilder.paymentMethodRemoveLastFeature(FeatureState.Enabled)
        checkoutRequestBuilder.paymentMethodSaveFeature(FeatureState.Enabled)
        checkoutRequestBuilder.paymentMethodOverrideRedisplay(null)
        checkoutRequestBuilder.paymentMethodSetAsDefaultFeature(FeatureState.Enabled)
        checkoutRequestBuilder.useLink(true)
        checkoutRequestBuilder.supportedPaymentMethods(listOf("card", "us_bank_account", "link"))
    }
}
