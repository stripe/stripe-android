package com.stripe.android.networktesting

import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.path
import okhttp3.mockwebserver.MockResponse

/** Enqueues a Link account lookup, returning no existing account unless a response is supplied. */
fun NetworkRule.enqueueConsumerSessionLookup(
    vararg requestMatchers: RequestMatcher,
    ensureResponseIsValidJson: Boolean = true,
    responseFactory: (MockResponse) -> Unit = { response ->
        response.setBody("""{"exists":"false"}""")
    },
) {
    enqueue(
        method("POST"),
        path("/v1/consumers/sessions/lookup"),
        *requestMatchers,
        ensureResponseIsValidJson = ensureResponseIsValidJson,
        responseFactory = responseFactory,
    )
}
