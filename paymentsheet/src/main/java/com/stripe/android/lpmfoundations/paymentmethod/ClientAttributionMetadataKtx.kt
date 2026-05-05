package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.PaymentIntentCreationFlow
import com.stripe.android.model.PaymentMethodSelectionFlow
import com.stripe.android.paymentsheet.state.PaymentElementLoader

internal fun ClientAttributionMetadata.Companion.create(
    elementsSessionConfigId: String?,
    initializationMode: PaymentElementLoader.InitializationMode,
    automaticPaymentMethodsEnabled: Boolean,
): ClientAttributionMetadata {
    val paymentIntentCreationFlow = when (initializationMode) {
        is PaymentElementLoader.InitializationMode.CryptoOnramp,
        is PaymentElementLoader.InitializationMode.DeferredIntent -> PaymentIntentCreationFlow.Deferred
        is PaymentElementLoader.InitializationMode.PaymentIntent,
        is PaymentElementLoader.InitializationMode.SetupIntent -> PaymentIntentCreationFlow.Standard
        is PaymentElementLoader.InitializationMode.CheckoutSession -> null
    }

    val paymentMethodSelectionFlow = if (automaticPaymentMethodsEnabled) {
        PaymentMethodSelectionFlow.Automatic
    } else {
        PaymentMethodSelectionFlow.MerchantSpecified
    }

    val checkoutSessionId =
        (initializationMode as? PaymentElementLoader.InitializationMode.CheckoutSession)
            ?.checkoutSessionResponse?.id

    return ClientAttributionMetadata(
        elementsSessionConfigId = elementsSessionConfigId,
        paymentIntentCreationFlow = paymentIntentCreationFlow,
        paymentMethodSelectionFlow = paymentMethodSelectionFlow,
        checkoutSessionId = checkoutSessionId,
    )
}
