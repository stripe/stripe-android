package com.stripe.android.paymentsheet

import com.stripe.android.model.DeferredIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent

internal fun PaymentSheet.IntentConfiguration.toDeferredIntentParams(): DeferredIntentParams {
    return DeferredIntentParams(
        mode = mode.toDeferredIntentMode(),
        paymentMethodTypes = paymentMethodTypes,
        onBehalfOf = onBehalfOf,
        paymentMethodConfigurationId = paymentMethodConfigurationId,
    )
}

private fun PaymentSheet.IntentConfiguration.Mode.toDeferredIntentMode(): DeferredIntentParams.Mode {
    return when (this) {
        is PaymentSheet.IntentConfiguration.Mode.Payment -> {
            DeferredIntentParams.Mode.Payment(
                amount = amount,
                currency = currency,
                setupFutureUsage = setupFutureUse?.toIntentUsage(),
                captureMethod = captureMethod.toIntentCaptureMethod(),
            )
        }
        is PaymentSheet.IntentConfiguration.Mode.Setup -> {
            DeferredIntentParams.Mode.Setup(
                currency = currency,
                setupFutureUsage = setupFutureUse.toIntentUsage(),
            )
        }
    }
}

private fun PaymentSheet.IntentConfiguration.SetupFutureUse.toIntentUsage(): StripeIntent.Usage {
    return when (this) {
        PaymentSheet.IntentConfiguration.SetupFutureUse.OnSession -> StripeIntent.Usage.OnSession
        PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession -> StripeIntent.Usage.OffSession
    }
}

private fun PaymentSheet.IntentConfiguration.CaptureMethod.toIntentCaptureMethod(): PaymentIntent.CaptureMethod {
    return when (this) {
        PaymentSheet.IntentConfiguration.CaptureMethod.Automatic -> PaymentIntent.CaptureMethod.Automatic
        PaymentSheet.IntentConfiguration.CaptureMethod.AutomaticAsync -> PaymentIntent.CaptureMethod.AutomaticAsync
        PaymentSheet.IntentConfiguration.CaptureMethod.Manual -> PaymentIntent.CaptureMethod.Manual
    }
}
