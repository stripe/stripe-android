package com.stripe.android.lpmfoundations.paymentmethod

import com.stripe.android.model.ClientAttributionMetadata
import com.stripe.android.model.PaymentIntentCreationFlow
import com.stripe.android.paymentsheet.state.PaymentElementLoader

internal fun ClientAttributionMetadata.Companion.create(
    elementsSessionConfigId: String,
    initializationMode: PaymentElementLoader.InitializationMode,
): ClientAttributionMetadata {
    val paymentIntentCreationFlow = when (initializationMode) {
        is PaymentElementLoader.InitializationMode.DeferredIntent -> PaymentIntentCreationFlow.Deferred
        is PaymentElementLoader.InitializationMode.PaymentIntent,
        is PaymentElementLoader.InitializationMode.SetupIntent -> PaymentIntentCreationFlow.Standard
    }

    return ClientAttributionMetadata(
        elementsSessionConfigId,
        paymentIntentCreationFlow,
    )
}