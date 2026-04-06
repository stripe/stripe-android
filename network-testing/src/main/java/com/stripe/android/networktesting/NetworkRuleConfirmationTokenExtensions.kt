package com.stripe.android.networktesting

import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import okhttp3.mockwebserver.MockResponse

fun NetworkRule.createConfirmationToken(
    vararg requestMatchers: RequestMatcher,
    responseFactory: (MockResponse) -> Unit,
) {
    enqueue(
        host("api.stripe.com"),
        method("POST"),
        path("/v1/confirmation_tokens"),
        *requestMatchers,
        responseFactory = responseFactory,
    )
}
