package com.stripe.android.paymentsheet.utils

import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.testBodyFromFile
import com.stripe.android.paymentsheet.confirmSetupIntentParams
import com.stripe.android.paymentsheet.confirmSetupIntentRequest
import com.stripe.android.paymentsheet.retrieveSetupIntentParams
import com.stripe.android.paymentsheet.retrieveSetupIntentRequest

internal object CustomerSheetUtils {
    fun enqueueFetchRequests(
        networkRule: NetworkRule,
        withCards: Boolean
    ) = with(networkRule) {
        enqueue(
            retrievePaymentMethodsRequest(),
            cardPaymentMethodsParams(),
        ) { response ->
            val file = if (withCards) {
                "payment-methods-get-success.json"
            } else {
                "payment-methods-get-success-empty.json"
            }

            response.testBodyFromFile(file)
        }

        enqueue(
            retrievePaymentMethodsRequest(),
            usBankAccountPaymentMethodsParams(),
        ) { response ->
            response.testBodyFromFile("payment-methods-get-success-empty.json")
        }
    }

    internal fun enqueueAttachRequests(
        networkRule: NetworkRule,
        customerSheetTestType: CustomerSheetTestType
    ) = with(networkRule) {
        when (customerSheetTestType) {
            CustomerSheetTestType.AttachToCustomer -> {
                enqueue(
                    attachPaymentMethodRequest(),
                ) { response ->
                    response.testBodyFromFile("payment-methods-create.json")
                }
            }
            CustomerSheetTestType.AttachToSetupIntent -> {
                enqueue(
                    retrieveSetupIntentRequest(),
                    retrieveSetupIntentParams(),
                ) { response ->
                    response.testBodyFromFile("setup-intent-get.json")
                }

                enqueue(
                    confirmSetupIntentRequest(),
                    confirmSetupIntentParams()
                ) { response ->
                    response.testBodyFromFile("setup-intent-confirm.json")
                }
            }
            CustomerSheetTestType.CustomerSession -> Unit
        }
    }

    internal fun retrieveElementsSessionRequest(): RequestMatcher {
        return RequestMatchers.composite(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/elements/sessions"),
        )
    }

    internal fun detachRequest(): RequestMatcher {
        return RequestMatchers.composite(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_methods/pm_67890/detach")
        )
    }

    private fun retrievePaymentMethodsRequest(): RequestMatcher {
        return RequestMatchers.composite(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/payment_methods"),
        )
    }

    private fun attachPaymentMethodRequest(): RequestMatcher {
        return RequestMatchers.composite(
            host("api.stripe.com"),
            method("POST"),
            path("/v1/payment_methods/pm_12345/attach"),
        )
    }

    private fun cardPaymentMethodsParams(): RequestMatcher {
        return RequestMatchers.composite(
            query("type", "card"),
        )
    }

    private fun usBankAccountPaymentMethodsParams(): RequestMatcher {
        return RequestMatchers.composite(
            query("type", "us_bank_account"),
        )
    }
}
