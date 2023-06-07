package com.stripe.android.paymentsheet

import com.stripe.android.model.DeferredIntentParams
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.repositories.toElementsSessionParams

@OptIn(ExperimentalPaymentSheetDecouplingApi::class)
internal object DeferredIntentValidator {

    fun validate(
        stripeIntent: StripeIntent,
        intentConfiguration: PaymentSheet.IntentConfiguration,
    ): StripeIntent {
        val params = mapToDeferredIntentParams(intentConfiguration)

        when (stripeIntent) {
            is PaymentIntent -> {
                val paymentMode = requireNotNull(params.mode as? DeferredIntentParams.Mode.Payment) {
                    "You returned a PaymentIntent client secret " +
                        "but used a PaymentSheet.IntentConfiguration in setup mode."
                }

                require(paymentMode.currency == stripeIntent.currency) {
                    "Your PaymentIntent currency (${stripeIntent.currency}) does not match " +
                        "the PaymentSheet.IntentConfiguration currency (${paymentMode.currency}))."
                }

                require(paymentMode.setupFutureUsage == stripeIntent.setupFutureUsage) {
                    "Your PaymentIntent setupFutureUsage (${stripeIntent.setupFutureUsage}) " +
                        "does not match the PaymentSheet.IntentConfiguration " +
                        "setupFutureUsage (${paymentMode.setupFutureUsage})."
                }

                require(paymentMode.amount == stripeIntent.amount) {
                    "Your PaymentIntent amount (${stripeIntent.amount}) does not match " +
                        "the PaymentSheet.IntentConfiguration amount (${paymentMode.amount})."
                }

                require(paymentMode.captureMethod == stripeIntent.captureMethod) {
                    "Your PaymentIntent captureMethod (${stripeIntent.captureMethod}) does not " +
                        "match the PaymentSheet.IntentConfiguration " +
                        "captureMethod (${paymentMode.captureMethod})."
                }
            }
            is SetupIntent -> {
                val setupMode = requireNotNull(params.mode as? DeferredIntentParams.Mode.Setup) {
                    "You returned a SetupIntent client secret " +
                        "but used a PaymentSheet.IntentConfiguration in payment mode."
                }

                require(setupMode.setupFutureUsage == stripeIntent.usage) {
                    "Your SetupIntent usage (${stripeIntent.usage}) does not match " +
                        "the PaymentSheet.IntentConfiguration usage (${stripeIntent.usage})."
                }
            }
        }

        return stripeIntent
    }

    private fun mapToDeferredIntentParams(
        intentConfiguration: PaymentSheet.IntentConfiguration,
    ): DeferredIntentParams {
        val initializationMode = PaymentSheet.InitializationMode.DeferredIntent(intentConfiguration)
        val params = initializationMode.toElementsSessionParams()
        return (params as ElementsSessionParams.DeferredIntentType).deferredIntentParams
    }
}
