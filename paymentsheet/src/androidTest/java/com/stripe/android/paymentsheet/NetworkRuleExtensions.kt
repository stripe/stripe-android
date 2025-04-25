package com.stripe.android.paymentsheet

import com.stripe.android.core.utils.urlEncode
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.query

internal fun NetworkRule.validateAnalyticsRequest(
    eventName: String,
    productUsage: Set<String>,
    vararg requestMatchers: RequestMatcher,
) {
    enqueue(
        host("q.stripe.com"),
        method("GET"),
        query("event", eventName),
        query("product_usage", urlEncode(productUsage.joinToString(","))),
        *requestMatchers,
    ) { response ->
        response.status = "HTTP/1.1 200 OK"
    }
}
