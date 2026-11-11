package com.stripe.android.checkout

import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.analyticsPayloadField
import com.stripe.android.networktesting.RequestMatchers.hasQueryParam
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.paymentsheet.validateAnalyticsRequest

/**
 * Validates the init started and succeeded events, which always carry the attempted configuration.
 */
internal fun NetworkRule.validateCheckoutInitStartedAndSucceeded(
    vararg requestMatchers: RequestMatcher,
) {
    validateCheckoutAnalyticsStarted("init", hasQueryParam("merchant_display_name"), *requestMatchers)
    validateCheckoutAnalyticsSucceeded("init", hasQueryParam("merchant_display_name"), *requestMatchers)
}

internal fun NetworkRule.validateCheckoutAnalyticsStarted(
    operation: String,
    vararg requestMatchers: RequestMatcher,
) {
    validateCheckoutAnalytics(operation, "started", not(hasQueryParam("duration")), *requestMatchers)
}

internal fun NetworkRule.validateCheckoutAnalyticsSucceeded(
    operation: String,
    vararg requestMatchers: RequestMatcher,
) {
    validateCheckoutAnalytics(operation, "succeeded", hasQueryParam("duration"), *requestMatchers)
}

internal fun NetworkRule.validateCheckoutAnalyticsFailed(
    operation: String,
    vararg requestMatchers: RequestMatcher,
) {
    validateCheckoutAnalytics(
        operation,
        "failed",
        hasQueryParam("duration"),
        hasQueryParam("error_type"),
        *requestMatchers,
    )
}

internal fun NetworkRule.validateCheckoutAnalyticsCanceled(
    operation: String,
    vararg requestMatchers: RequestMatcher,
) {
    validateCheckoutAnalytics(operation, "canceled", hasQueryParam("duration"), *requestMatchers)
}

private fun NetworkRule.validateCheckoutAnalytics(
    operation: String,
    outcome: String,
    vararg requestMatchers: RequestMatcher,
) {
    validateCheckoutSessionAnalyticsRequest("checkout_elements.$operation.$outcome", *requestMatchers)
}

internal fun NetworkRule.validateCheckoutSessionAnalyticsRequest(
    eventName: String,
    vararg requestMatchers: RequestMatcher,
) {
    validateAnalyticsRequest(
        eventName = eventName,
        productUsage = setOf("checkout_elements"),
        analyticsPayloadField("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID),
        *requestMatchers,
    )
}
