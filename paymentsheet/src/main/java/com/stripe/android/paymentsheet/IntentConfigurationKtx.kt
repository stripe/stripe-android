package com.stripe.android.paymentsheet

import com.stripe.android.elements.payment.IntentConfiguration
import com.stripe.android.elements.payment.PaymentMethodOptionsSetupFutureUsagePreview
import com.stripe.android.model.DeferredIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.paymentmethodoptions.setupfutureusage.toJsonObjectString

internal fun IntentConfiguration.toDeferredIntentParams(): DeferredIntentParams {
    return DeferredIntentParams(
        mode = mode.toDeferredIntentMode(),
        paymentMethodTypes = paymentMethodTypes,
        onBehalfOf = onBehalfOf,
        paymentMethodConfigurationId = paymentMethodConfigurationId,
    )
}

@OptIn(PaymentMethodOptionsSetupFutureUsagePreview::class)
private fun IntentConfiguration.Mode.toDeferredIntentMode(): DeferredIntentParams.Mode {
    return when (this) {
        is IntentConfiguration.Mode.Payment -> {
            DeferredIntentParams.Mode.Payment(
                amount = amount,
                currency = currency,
                setupFutureUsage = setupFutureUse?.toIntentUsage(),
                captureMethod = captureMethod.toIntentCaptureMethod(),
                paymentMethodOptionsJsonString = paymentMethodOptions?.toJsonObjectString()
            )
        }
        is IntentConfiguration.Mode.Setup -> {
            DeferredIntentParams.Mode.Setup(
                currency = currency,
                setupFutureUsage = setupFutureUse.toIntentUsage(),
            )
        }
    }
}

private fun IntentConfiguration.SetupFutureUse.toIntentUsage(): StripeIntent.Usage {
    return when (this) {
        IntentConfiguration.SetupFutureUse.OnSession -> StripeIntent.Usage.OnSession
        IntentConfiguration.SetupFutureUse.OffSession -> StripeIntent.Usage.OffSession
        IntentConfiguration.SetupFutureUse.None -> throw IllegalArgumentException(
            "IntentConfiguration setupFutureUse cannot be set to None"
        )
    }
}

private fun IntentConfiguration.CaptureMethod.toIntentCaptureMethod(): PaymentIntent.CaptureMethod {
    return when (this) {
        IntentConfiguration.CaptureMethod.Automatic -> PaymentIntent.CaptureMethod.Automatic
        IntentConfiguration.CaptureMethod.AutomaticAsync -> PaymentIntent.CaptureMethod.AutomaticAsync
        IntentConfiguration.CaptureMethod.Manual -> PaymentIntent.CaptureMethod.Manual
    }
}
