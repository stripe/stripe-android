package com.stripe.android.networktesting

import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import okhttp3.mockwebserver.MockResponse

fun NetworkRule.elementsSession(
    vararg requestMatchers: RequestMatcher,
    responseFactory: (MockResponse) -> Unit,
) {
    enqueue(
        host("api.stripe.com"),
        method("GET"),
        path("/v1/elements/sessions"),
        *requestMatchers,
        responseFactory = responseFactory,
    )
}
