package com.stripe.android.paymentsheet.utils

import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import com.stripe.android.core.utils.urlEncode
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.bodyPart
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.not
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
        paymentMethodType: PaymentMethodType = PaymentMethodType.Card,
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
            paymentMethodType: PaymentMethodType,
            setAsDefault: Boolean
        ) {
            if (paymentMethodType is PaymentMethodType.Card) {
                enqueuePaymentIntentConfirmWithExpectedSetAsDefault_Card(
                    networkRule = networkRule,
                    setAsDefault = setAsDefault,
                )
            } else {
                enqueuePaymentIntentConfirmWithExpectedSetAsDefault_USBankAccount(
                    networkRule = networkRule,
                    setAsDefault = setAsDefault,
                )
            }
        }

        private fun enqueuePaymentIntentConfirmWithExpectedSetAsDefault_Card(
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

        private fun enqueuePaymentIntentConfirmWithExpectedSetAsDefault_USBankAccount(
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

        override fun enqueuePaymentIntentConfirmWithoutSetAsDefault(networkRule: NetworkRule) {
            return networkRule.enqueue(
                method("POST"),
                path("/v1/payment_intents/pi_example/confirm"),
                bodyPart(urlEncode("payment_method_data[allow_redisplay]"), "unspecified"),
                not(bodyPart(urlEncode("set_as_default_payment_method"), "true")),
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
            paymentMethodType: PaymentMethodType,
            setAsDefault: Boolean
        ) {
            if (paymentMethodType is PaymentMethodType.Card) {
                enqueuePaymentIntentConfirmWithExpectedSetAsDefault_Card(
                    networkRule = networkRule,
                    setAsDefault = setAsDefault,
                )
            } else {
                enqueuePaymentIntentConfirmWithExpectedSetAsDefault_USBankAccount(
                    networkRule = networkRule,
                    setAsDefault = setAsDefault,
                )
            }
        }

        private fun enqueuePaymentIntentConfirmWithExpectedSetAsDefault_Card(
            networkRule: NetworkRule,
            setAsDefault: Boolean
        ) {
            networkRule.enqueue(
                method("POST"),
                path("/v1/payment_methods"),
            ) { response ->
                response.testBodyFromFile("payment-methods-create-us_bank_account.json")
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

        private fun enqueuePaymentIntentConfirmWithExpectedSetAsDefault_USBankAccount(
            networkRule: NetworkRule,
            setAsDefault: Boolean
        ) {
            networkRule.enqueue(
                method("POST"),
                path("/v1/payment_methods"),
            ) { response ->
                response.testBodyFromFile("payment-methods-create-us_bank_account.json")
            }

            networkRule.enqueue(
                method("GET"),
                path("/v1/payment_intents/pi_example"),
            ) { response ->
                response.testBodyFromFile("payment-intent-get-requires_payment_method-us_bank_account.json")
            }

            networkRule.enqueue(
                method("POST"),
                path("/v1/payment_intents/pi_example/confirm"),
                bodyPart(urlEncode("set_as_default_payment_method"), setAsDefault.toString()),
            ) { response ->
                response.testBodyFromFile("payment-intent-confirm-us_bank_account.json")
            }
        }

        override fun enqueuePaymentIntentConfirmWithoutSetAsDefault(networkRule: NetworkRule) {
            networkRule.enqueue(
                method("POST"),
                path("/v1/payment_methods"),
            ) { response ->
                response.testBodyFromFile("payment-methods-create-us_bank_account.json")
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
                not(bodyPart(urlEncode("set_as_default_payment_method"), "true")),
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
