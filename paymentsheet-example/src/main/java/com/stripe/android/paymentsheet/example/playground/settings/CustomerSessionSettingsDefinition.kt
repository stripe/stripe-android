package com.stripe.android.paymentsheet.example.playground.settings

import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.CustomerEphemeralKeyRequest

internal object CustomerSessionSettingsDefinition : BooleanSettingsDefinition(
    defaultValue = true,
    displayName = "Use Customer Session",
    key = "customer_session_enabled"
) {
    override fun applicable(configurationData: PlaygroundConfigurationData): Boolean {
        return configurationData.integrationType.isPaymentFlow() ||
            configurationData.integrationType.isCustomerFlow()
    }

    override fun configure(value: Boolean, checkoutRequestBuilder: CheckoutRequest.Builder) {
        if (value) {
            checkoutRequestBuilder.customerKeyType(CheckoutRequest.CustomerKeyType.CustomerSession)
        } else {
            checkoutRequestBuilder.customerKeyType(CheckoutRequest.CustomerKeyType.Legacy)
        }
    }

    override fun valueUpdated(value: Boolean, playgroundSettings: PlaygroundSettings) {
        if (!value) {
            playgroundSettings[CustomerSessionOnBehalfOfSettingsDefinition] =
                CustomerSessionOnBehalfOfSettingsDefinition.OnBehalfOf.NO_CONNECTED_ACCOUNT
        }
    }

    override fun configure(
        value: Boolean,
        customerEphemeralKeyRequestBuilder: CustomerEphemeralKeyRequest.Builder
    ) {
        if (value) {
            customerEphemeralKeyRequestBuilder.customerKeyType(
                CustomerEphemeralKeyRequest.CustomerKeyType.CustomerSession
            )
        } else {
            customerEphemeralKeyRequestBuilder.customerKeyType(
                CustomerEphemeralKeyRequest.CustomerKeyType.Legacy
            )
        }
    }
}
