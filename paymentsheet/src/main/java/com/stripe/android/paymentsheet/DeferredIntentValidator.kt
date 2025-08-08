package com.stripe.android.paymentsheet

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.model.DeferredIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntent.ConfirmationMethod.Manual
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import org.json.JSONObject

internal object DeferredIntentValidator {

    /**
     * Validates that the created [StripeIntent] matches the [PaymentSheet.IntentConfiguration] that
     * was provided to [PaymentSheet].
     */
    fun validate(
        stripeIntent: StripeIntent,
        intentConfiguration: PaymentSheet.IntentConfiguration,
        allowsManualConfirmation: Boolean,
    ): StripeIntent {
        val params = intentConfiguration.toDeferredIntentParams()

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

//                require(paymentMode.setupFutureUsage.isNull() == stripeIntent.setupFutureUsage.isNull()) {
//                    "Your PaymentIntent setupFutureUsage (${stripeIntent.setupFutureUsage}) " +
//                        "does not match the PaymentSheet.IntentConfiguration " +
//                        "setupFutureUsage (${paymentMode.setupFutureUsage})."
//                }

                // Manual confirmation is only available using FlowController because merchants own
                // the final step of confirmation. Showing a successful payment in the complete flow
                // may be misleading when merchants still need to do a final confirmation which
                // could fail.
                require(stripeIntent.confirmationMethod != Manual || allowsManualConfirmation) {
                    "Your PaymentIntent confirmationMethod (${stripeIntent.confirmationMethod}) " +
                        "can only be used with PaymentSheet.FlowController."
                }

//                require(
//                    validatePaymentMethodOptionsSetupFutureUsage(
//                        paramsPaymentMethodOptionsJsonString = paymentMode.paymentMethodOptionsJsonString,
//                        stripeIntent = stripeIntent
//                    )
//                ) {
//                    "Your PaymentIntent payment_method_options setup_future_usage values " +
//                        "(${stripeIntent.getPaymentMethodOptions()} do not match the values provided in " +
//                        "PaymentSheet.IntentConfiguration.Mode.Payment.PaymentMethodOptions " +
//                        "(${paymentMode.paymentMethodOptionsJsonString})"
//                }
            }
            is SetupIntent -> {
                val setupMode = requireNotNull(params.mode as? DeferredIntentParams.Mode.Setup) {
                    "You returned a SetupIntent client secret " +
                        "but used a PaymentSheet.IntentConfiguration in payment mode."
                }

                require(setupMode.setupFutureUsage.isNull() == stripeIntent.usage.isNull()) {
                    "Your SetupIntent usage (${stripeIntent.usage}) does not match " +
                        "the PaymentSheet.IntentConfiguration usage (${stripeIntent.usage})."
                }
            }
        }

        return stripeIntent
    }

    fun validatePaymentMethod(
        intent: StripeIntent,
        paymentMethod: PaymentMethod
    ) {
        val attachedPaymentMethod = intent.paymentMethodId ?: intent.paymentMethod?.id

        require(
            attachedPaymentMethod == null ||
                attachedPaymentMethod == paymentMethod.id ||
                isSimilarPaymentMethod(paymentMethod, intent.paymentMethod)
        ) {
            "Your payment method ($attachedPaymentMethod) attached to the intent does not match the " +
                "provided payment method (${paymentMethod.id})!"
        }
    }

    private fun isSimilarPaymentMethod(
        providedPaymentMethod: PaymentMethod,
        attachedPaymentMethod: PaymentMethod?,
    ): Boolean {
        if (
            attachedPaymentMethod == null ||
            providedPaymentMethod.type != attachedPaymentMethod.type
        ) {
            return false
        }

        return when (providedPaymentMethod.type) {
            PaymentMethod.Type.Card -> isSameFingerprint(providedPaymentMethod, attachedPaymentMethod) {
                card?.fingerprint
            }
            PaymentMethod.Type.USBankAccount -> isSameFingerprint(providedPaymentMethod, attachedPaymentMethod) {
                usBankAccount?.fingerprint
            }
            PaymentMethod.Type.AuBecsDebit -> isSameFingerprint(providedPaymentMethod, attachedPaymentMethod) {
                auBecsDebit?.fingerprint
            }
            PaymentMethod.Type.BacsDebit -> isSameFingerprint(providedPaymentMethod, attachedPaymentMethod) {
                bacsDebit?.fingerprint
            }
            PaymentMethod.Type.SepaDebit -> isSameFingerprint(providedPaymentMethod, attachedPaymentMethod) {
                sepaDebit?.fingerprint
            }
            else -> false
        }
    }

    private fun isSameFingerprint(
        firstPaymentMethod: PaymentMethod,
        secondPaymentMethod: PaymentMethod,
        fingerprintProvider: PaymentMethod.() -> String?,
    ): Boolean {
        val firstFingerprint = fingerprintProvider(firstPaymentMethod)
        val secondFingerprint = fingerprintProvider(secondPaymentMethod)

        if (firstFingerprint == null || secondFingerprint == null) {
            return false
        }

        return firstFingerprint == secondFingerprint
    }

    private fun validatePaymentMethodOptionsSetupFutureUsage(
        paramsPaymentMethodOptionsJsonString: String?,
        stripeIntent: StripeIntent,
    ): Boolean {
        val paramsMap = paramsPaymentMethodOptionsJsonString?.let {
            StripeJsonUtils.jsonObjectToMap(JSONObject(it))
        } ?: emptyMap()

        return paramsMap.entries.all { (key, value) ->
            val paramsSfu = (value as? Map<*, *>?)?.get("setup_future_usage") as? String
            val intentSfu = (stripeIntent.getPaymentMethodOptions()[key] as? Map<*, *>?)?.get("setup_future_usage")
            val isPaymentMethodInIntent = stripeIntent.paymentMethodTypes.contains(key)
            if (isPaymentMethodInIntent) intentSfu == paramsSfu else true
        }
    }
}

private fun Any?.isNull(): Boolean {
    return this == null
}
