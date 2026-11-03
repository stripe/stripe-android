package com.stripe.android.checkouttesting

import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import com.stripe.android.networktesting.testBodyFromFile
import okhttp3.mockwebserver.MockResponse
import org.json.JSONObject

const val DEFAULT_CHECKOUT_SESSION_ID = "cs_test_abc123"

fun NetworkRule.createPaymentMethod(
    vararg requestMatchers: RequestMatcher,
    responseFactory: (MockResponse) -> Unit = { response ->
        response.testBodyFromFile("payment-methods-create.json")
    },
) {
    enqueue(
        host("api.stripe.com"),
        method("POST"),
        path("/v1/payment_methods"),
        *requestMatchers,
        responseFactory = responseFactory,
    )
}

fun NetworkRule.checkoutInit(
    vararg requestMatchers: RequestMatcher,
    sessionId: String = DEFAULT_CHECKOUT_SESSION_ID,
    responseFactory: (MockResponse) -> Unit = CheckoutInitResponseFactory::create,
) {
    enqueue(
        host("api.stripe.com"),
        method("POST"),
        path("/v1/checkout_client/$sessionId/init"),
        *requestMatchers,
        responseFactory = responseFactory,
    )
}

fun NetworkRule.checkoutUpdate(
    vararg requestMatchers: RequestMatcher,
    sessionId: String = DEFAULT_CHECKOUT_SESSION_ID,
    responseFactory: (MockResponse) -> Unit,
) {
    enqueue(
        host("api.stripe.com"),
        method("POST"),
        path("/v1/checkout_client/$sessionId/update"),
        *requestMatchers,
        responseFactory = responseFactory,
    )
}

fun NetworkRule.checkoutConfirm(
    vararg requestMatchers: RequestMatcher,
    sessionId: String = DEFAULT_CHECKOUT_SESSION_ID,
    intentResponseFactory: ((MockResponse) -> Unit)? = null,
    responseFactory: (MockResponse) -> Unit,
) {
    val checkoutResponse = MockResponse().also(responseFactory)
    val json = checkoutResponse.getBody()?.clone()?.readUtf8()?.let { JSONObject(it) }
    val paymentResponse = json?.optJSONObject("payment_response")
    val paymentIntent = paymentResponse?.optJSONObject("payment_intent")
    val setupIntent = paymentResponse?.optJSONObject("setup_intent")
    val reference = paymentIntent ?: setupIntent
    if (reference != null && paymentResponse?.optString("status") != "requires_payment_method") {
        val intentType = if (paymentIntent != null) "payment" else "setup"
        enqueue(
            host("api.stripe.com"),
            method("GET"),
            path("/v1/${intentType}_intents/${reference.getString("id")}"),
            query("client_secret", reference.getString("client_secret")),
            query("expand[]", "payment_method"),
        ) { response ->
            if (intentResponseFactory != null) {
                intentResponseFactory(response)
            } else {
                response.testBodyFromFile("checkout-$intentType-intent.json") { intent ->
                    intent.put("id", reference.getString("id"))
                    intent.put("client_secret", reference.getString("client_secret"))
                }
            }
        }
    }
    enqueue(
        host("api.stripe.com"),
        method("POST"),
        path("/v1/checkout_client/$sessionId/confirm"),
        *requestMatchers,
        responseFactory = { response ->
            response.setStatus(checkoutResponse.status)
            checkoutResponse.getBody()?.let { response.setBody(it.clone()) }
        },
    )
}

fun NetworkRule.checkoutPoll(
    vararg requestMatchers: RequestMatcher,
    sessionId: String = DEFAULT_CHECKOUT_SESSION_ID,
    responseFactory: (MockResponse) -> Unit = { response ->
        response.testBodyFromFile("checkout-session-poll.json")
    },
) {
    enqueue(
        host("api.stripe.com"),
        method("GET"),
        path("/v1/checkout_client/$sessionId/poll"),
        *requestMatchers,
        responseFactory = responseFactory,
    )
}
