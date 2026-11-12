package com.stripe.android.checkouttesting

import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.testBodyFromFile
import okhttp3.mockwebserver.MockResponse

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

fun NetworkRule.checkoutFetch(
    vararg requestMatchers: RequestMatcher,
    sessionId: String = DEFAULT_CHECKOUT_SESSION_ID,
    responseFactory: (MockResponse) -> Unit = CheckoutInitResponseFactory::create,
) {
    enqueue(
        host("api.stripe.com"),
        method("GET"),
        path("/v1/checkout_client/$sessionId/retrieve"),
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
    responseFactory: (MockResponse) -> Unit,
) {
    enqueue(
        host("api.stripe.com"),
        method("POST"),
        path("/v1/checkout_client/$sessionId/confirm"),
        *requestMatchers,
        responseFactory = responseFactory,
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
