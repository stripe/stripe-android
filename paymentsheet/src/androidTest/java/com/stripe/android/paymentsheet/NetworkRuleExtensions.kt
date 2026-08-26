package com.stripe.android.paymentsheet

import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.analyticsPayloadField
import com.stripe.android.networktesting.RequestMatchers.stripeApiKey
import com.stripe.android.networktesting.TestApiKeys

internal fun NetworkRule.validateAnalyticsRequest(
    eventName: String,
    productUsage: Set<String>,
    vararg requestMatchers: RequestMatcher,
    publishableKey: String = TestApiKeys.PUBLISHABLE,
) {
    enqueue(
        stripeApiKey(publishableKey = publishableKey),
        host("q.stripe.com"),
        method("GET"),
        analyticsPayloadField("event", eventName),
        analyticsPayloadField("product_usage", productUsage.joinToString(",")),
        *requestMatchers,
        applyDefaultAuthorization = false,
    ) { response ->
        response.status = "HTTP/1.1 200 OK"
    }
}
