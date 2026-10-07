package com.stripe.android.paymentelement

import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks

@OptIn(
    ExperimentalAnalyticEventCallbackApi::class,
    SharedPaymentTokenSessionPreview::class,
    TapToAddPreview::class,
)
internal fun EmbeddedPaymentElement.Builder.createCallbacks(
    element: () -> EmbeddedPaymentElement,
): PaymentElementCallbacks {
    return PaymentElementCallbacks.Builder()
        .apply {
            when (val handler = deferredHandler) {
                is EmbeddedPaymentElement.Builder.DeferredHandler.Intent -> {
                    createIntentCallback(handler.createIntentCallback)
                }
                is EmbeddedPaymentElement.Builder.DeferredHandler.ConfirmationToken -> {
                    createIntentCallback(handler.createIntentWithConfirmationTokenCallback)
                }
                is EmbeddedPaymentElement.Builder.DeferredHandler.SharedPaymentToken -> {
                    preparePaymentMethodHandler(handler.preparePaymentMethodHandler)
                }
            }
        }
        .confirmCustomPaymentMethodCallback(confirmCustomPaymentMethodCallback)
        .externalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)
        .analyticEventCallback(analyticEventCallback)
        .createCardPresentSetupIntentCallback(createCardPresentSetupIntentCallback)
        .rowSelectionImmediateActionCallback(rowSelectionBehavior, element)
        .build()
}
