package com.stripe.android.lpmfoundations.paymentmethod

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
        is PaymentElementLoader.InitializationMode.CryptoOnramp,
        is PaymentElementLoader.InitializationMode.StandaloneLink,
        is PaymentElementLoader.InitializationMode.DeferredIntent -> PaymentIntentCreationFlow.Deferred
        is PaymentElementLoader.InitializationMode.PaymentIntent,
        is PaymentElementLoader.InitializationMode.SetupIntent -> PaymentIntentCreationFlow.Standard
        is PaymentElementLoader.InitializationMode.CheckoutSession -> null
    }

    val checkout = initializationMode as? PaymentElementLoader.InitializationMode.CheckoutSession
    val automaticPaymentMethodTypes = if (checkout != null) {
        checkout.checkoutSessionResponse.automaticPaymentMethodTypes
    } else {
        automaticPaymentMethodsEnabled
    }
    val paymentMethodSelectionFlow = when (automaticPaymentMethodTypes) {
        true -> PaymentMethodSelectionFlow.Automatic
        false -> PaymentMethodSelectionFlow.MerchantSpecified
        null -> null
    }

    return ClientAttributionMetadata(
        elementsSessionConfigId = elementsSessionConfigId,
        paymentIntentCreationFlow = paymentIntentCreationFlow,
        paymentMethodSelectionFlow = paymentMethodSelectionFlow,
        checkoutSessionId = checkout?.checkoutSessionResponse?.id,
        checkoutConfigId = checkout?.checkoutSessionResponse?.configId,
        merchantIntegrationSource = if (checkout != null) "mobile-checkout" else "elements",
        merchantIntegrationSubtype = when {
            checkout == null -> "mobile"
            isExpressCheckoutElement -> "mobile-express-checkout-element"
            else -> "mobile-payment-element"
        },
        merchantIntegrationAdditionalElements = checkout?.configuredElements,
    )
}
