package com.stripe.android.paymentsheet.utils

import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.CreateIntentResult

internal sealed class ConfirmationType(
    val createIntentCallback: CreateIntentCallback?,
    val isDeferredIntent: Boolean,
) {
    abstract fun enqueuePaymentIntentConfirmWithExpectedSetAsDefault(
        networkRule: NetworkRule,
        setAsDefault: Boolean,
    )

    abstract fun enqueuePaymentIntentConfirmWithoutSetAsDefault(
        networkRule: NetworkRule,
    )

    class IntentFirst : ConfirmationType(
        createIntentCallback = null,
        isDeferredIntent = false,
    ) {
        override fun enqueuePaymentIntentConfirmWithExpectedSetAsDefault(
            networkRule: NetworkRule,
            setAsDefault: Boolean
        ) {
            return networkRule.enqueue(
                method("POST"),
                path("/v1/payment_intents/pi_example/confirm"),
                bodyPart(urlEncode("payment_method_data[allow_redisplay]"), "always"),
                bodyPart(urlEncode("set_as_default_payment_method"), setAsDefault.toString())
            ) { response ->
                response.testBodyFromFile("payment-intent-confirm.json")
            }
        }

        // TODO: add a not set as default pm param check to this and to the one for deferred CSC?
        override fun enqueuePaymentIntentConfirmWithoutSetAsDefault(networkRule: NetworkRule) {
            return networkRule.enqueue(
                method("POST"),
                path("/v1/payment_intents/pi_example/confirm"),
                bodyPart(urlEncode("payment_method_data[allow_redisplay]"), "unspecified"),
            ) { response ->
                response.testBodyFromFile("payment-intent-confirm.json")
            }
        }
    }

    class DeferredClientSideConfirmation : ConfirmationType(
        createIntentCallback = { _, _ ->
            CreateIntentResult.Success(clientSecret = "pi_example_secret_example")
        },
        isDeferredIntent = true,
    ) {
        override fun enqueuePaymentIntentConfirmWithExpectedSetAsDefault(
            networkRule: NetworkRule,
            setAsDefault: Boolean
        ) {
            networkRule.enqueue(
                method("POST"),
                path("/v1/payment_methods"),
            ) { response ->
                response.testBodyFromFile("payment-methods-create.json")
            }

            networkRule.enqueue(
                method("GET"),
                path("/v1/payment_intents/pi_example"),
            ) { response ->
                response.testBodyFromFile("payment-intent-get-requires_payment_method.json")
            }

            networkRule.enqueue(
                method("POST"),
                path("/v1/payment_intents/pi_example/confirm"),
                bodyPart(urlEncode("set_as_default_payment_method"), setAsDefault.toString()),
            ) { response ->
                response.testBodyFromFile("payment-intent-confirm.json")
            }
        }

        override fun enqueuePaymentIntentConfirmWithoutSetAsDefault(networkRule: NetworkRule) {
            networkRule.enqueue(
                method("POST"),
                path("/v1/payment_methods"),
            ) { response ->
                response.testBodyFromFile("payment-methods-create.json")
            }

            networkRule.enqueue(
                method("GET"),
                path("/v1/payment_intents/pi_example"),
            ) { response ->
                response.testBodyFromFile("payment-intent-get-requires_payment_method.json")
            }

            networkRule.enqueue(
                method("POST"),
                path("/v1/payment_intents/pi_example/confirm"),
            ) { response ->
                response.testBodyFromFile("payment-intent-confirm.json")
            }
        }
    }
}

internal object ConfirmationTypeProvider : TestParameterValuesProvider() {
    override fun provideValues(context: Context?): List<ConfirmationType> {
        return listOf(
            ConfirmationType.IntentFirst(),
            ConfirmationType.DeferredClientSideConfirmation(),
        )
    }
}