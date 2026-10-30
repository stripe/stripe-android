package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.PaymentIntentCreationFlow
import com.stripe.android.model.PaymentMethodSelectionFlow
import com.stripe.android.paymentsheet.state.PaymentElementLoader

internal fun ClientAttributionMetadata.Companion.create(
    elementsSessionConfigId: String?,
    initializationMode: PaymentElementLoader.InitializationMode,
    automaticPaymentMethodsEnabled: Boolean,
    isExpressCheckoutElement: Boolean,
): ClientAttributionMetadata {
    val paymentIntentCreationFlow = when (initializationMode) {
        is PaymentElementLoader.InitializationMode.CheckoutSession -> null
        is PaymentElementLoader.InitializationMode.CryptoOnramp,
        is PaymentElementLoader.InitializationMode.StandaloneLink,
        is PaymentElementLoader.InitializationMode.DeferredIntent -> PaymentIntentCreationFlow.Deferred
        is PaymentElementLoader.InitializationMode.PaymentIntent,
        is PaymentElementLoader.InitializationMode.SetupIntent -> PaymentIntentCreationFlow.Standard
    }

    return when (initializationMode) {
        is PaymentElementLoader.InitializationMode.CheckoutSession -> {
            val response = initializationMode.checkoutSessionResponse
            ClientAttributionMetadata.CheckoutSession(
                elementsSessionConfigId = elementsSessionConfigId,
                paymentMethodSelectionFlow = when (response.automaticPaymentMethodTypes) {
                    true -> PaymentMethodSelectionFlow.Automatic
                    false -> PaymentMethodSelectionFlow.MerchantSpecified
                    null -> null
                },
                checkoutSessionId = response.id,
                checkoutConfigId = response.configId,
                configuredElements = initializationMode.configuredElements,
                isExpressCheckoutElement = isExpressCheckoutElement,
                stripeSdkVersion = StripeSdkVersion.VERSION_NAME,
            )
        }
        else -> ClientAttributionMetadata.Elements(
            elementsSessionConfigId = elementsSessionConfigId,
            paymentIntentCreationFlow = paymentIntentCreationFlow,
            paymentMethodSelectionFlow = if (automaticPaymentMethodsEnabled) {
                PaymentMethodSelectionFlow.Automatic
            } else {
                PaymentMethodSelectionFlow.MerchantSpecified
            },
            stripeSdkVersion = StripeSdkVersion.VERSION_NAME,
        )
    }
}
