package com.stripe.android.checkout

import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.analyticsPayloadField
import com.stripe.android.networktesting.RequestMatchers.hasQueryParam
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.paymentsheet.validateAnalyticsRequest

internal fun NetworkRule.validateCheckoutInitStarted(
    sessionId: String?,
    vararg requestMatchers: RequestMatcher,
) {
    validateCheckoutInitAnalyticsRequest(
        "checkout_elements.init.started",
        sessionId,
        *requestMatchers,
    )
}

internal fun NetworkRule.validateCheckoutInitSucceeded(
    sessionId: String?,
    vararg requestMatchers: RequestMatcher,
) {
    validateCheckoutInitAnalyticsRequest(
        "checkout_elements.init.succeeded",
        sessionId,
        *requestMatchers,
    )
}

internal fun NetworkRule.validateCheckoutInitFailed(
    sessionId: String?,
    vararg requestMatchers: RequestMatcher,
) {
    validateCheckoutInitAnalyticsRequest(
        "checkout_elements.init.failed",
        sessionId,
        *requestMatchers,
    )
}

private fun NetworkRule.validateCheckoutInitAnalyticsRequest(
    eventName: String,
    sessionId: String?,
    vararg requestMatchers: RequestMatcher,
) {
    if (sessionId == null) {
        validateAnalyticsRequest(
            eventName = eventName,
            productUsage = setOf("checkout_elements"),
            not(hasQueryParam("checkout_session_id")),
            *requestMatchers,
        )
    } else {
        validateAnalyticsRequest(
            eventName = eventName,
            productUsage = setOf("checkout_elements"),
            analyticsPayloadField("checkout_session_id", sessionId),
            *requestMatchers,
        )
    }
}
