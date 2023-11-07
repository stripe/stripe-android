package com.stripe.android.paymentsheet

import com.stripe.android.model.DeferredIntentParams
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntent.ConfirmationMethod.Manual
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.repositories.toElementsSessionParams

internal object DeferredIntentValidator {

    /**
     * Validates that the created [StripeIntent] matches the [PaymentSheet.IntentConfiguration] that
     * was provided to [PaymentSheet].
     */
    fun validate(
        stripeIntent: StripeIntent,
        intentConfiguration: PaymentSheet.IntentConfiguration,
        isFlowController: Boolean,
    ): StripeIntent {
        val params = mapToDeferredIntentParams(intentConfiguration)

        when (stripeIntent) {
            is PaymentIntent -> {
                val paymentMode = requireNotNull(params.mode as? DeferredIntentParams.Mode.Payment) {
                    "You returned a PaymentIntent client secret " +
                        "but used a PaymentSheet.IntentConfiguration in setup mode."
                }

                require(paymentMode.currency.lowercase() == stripeIntent.currency?.lowercase()) {
                    "Your PaymentIntent currency (${stripeIntent.currency?.lowercase()}) does " +
                        "not match the PaymentSheet.IntentConfiguration currency " +
                        "(${paymentMode.currency.lowercase()})."
                }

                require(paymentMode.setupFutureUsage == stripeIntent.setupFutureUsage) {
                    "Your PaymentIntent setupFutureUsage (${stripeIntent.setupFutureUsage}) " +
                        "does not match the PaymentSheet.IntentConfiguration " +
                        "setupFutureUsage (${paymentMode.setupFutureUsage})."
                }

                require(paymentMode.captureMethod == stripeIntent.captureMethod) {
                    "Your PaymentIntent captureMethod (${stripeIntent.captureMethod}) does not " +
                        "match the PaymentSheet.IntentConfiguration " +
                        "captureMethod (${paymentMode.captureMethod})."
                }

                // Manual confirmation is only available using FlowController because merchants own
                // the final step of confirmation. Showing a successful payment in the complete flow
                // may be misleading when merchants still need to do a final confirmation which
                // could fail.
                require(stripeIntent.confirmationMethod != Manual || isFlowController) {
                    "Your PaymentIntent confirmationMethod (${stripeIntent.confirmationMethod}) " +
                        "can only be used with PaymentSheet.FlowController."
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
