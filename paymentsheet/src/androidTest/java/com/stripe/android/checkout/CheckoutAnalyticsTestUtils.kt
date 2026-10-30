package com.stripe.android.checkout

import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
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
    validateCheckoutStartedAnalyticsRequest("checkout_elements.init.started", sessionId, *requestMatchers)
}

internal fun NetworkRule.validateCheckoutInitSucceeded(
    sessionId: String?,
    vararg requestMatchers: RequestMatcher,
) {
    validateCheckoutTerminalAnalyticsRequest("checkout_elements.init.succeeded", sessionId, *requestMatchers)
}

internal fun NetworkRule.validateCheckoutInitFailed(
    sessionId: String?,
    vararg requestMatchers: RequestMatcher,
) {
    validateCheckoutTerminalAnalyticsRequest("checkout_elements.init.failed", sessionId, *requestMatchers)
}

internal fun NetworkRule.validateConfirmationStartedAnalyticsRequest(
    invocationSource: String,
    selectedLpm: String,
    vararg requestMatchers: RequestMatcher,
) {
    validateCheckoutStartedAnalyticsRequest(
        "checkout_elements.confirm.started",
        DEFAULT_CHECKOUT_SESSION_ID,
        analyticsPayloadField("invocation_source", invocationSource),
        analyticsPayloadField("selected_lpm", selectedLpm),
        *requestMatchers,
    )
}

/**
 * Validates a terminal confirm event: succeeded, failed, or canceled.
 */
internal fun NetworkRule.validateConfirmationResultAnalyticsRequest(
    eventName: String,
    invocationSource: String,
    selectedLpm: String,
    vararg requestMatchers: RequestMatcher,
) {
    validateCheckoutTerminalAnalyticsRequest(
        eventName,
        DEFAULT_CHECKOUT_SESSION_ID,
        analyticsPayloadField("invocation_source", invocationSource),
        analyticsPayloadField("selected_lpm", selectedLpm),
        *requestMatchers,
    )
}

private fun NetworkRule.validateCheckoutStartedAnalyticsRequest(
    eventName: String,
    sessionId: String?,
    vararg requestMatchers: RequestMatcher,
) {
    validateCheckoutAnalyticsRequest(eventName, sessionId, not(hasQueryParam("duration")), *requestMatchers)
}

private fun NetworkRule.validateCheckoutTerminalAnalyticsRequest(
    eventName: String,
    sessionId: String?,
    vararg requestMatchers: RequestMatcher,
) {
    validateCheckoutAnalyticsRequest(eventName, sessionId, hasQueryParam("duration"), *requestMatchers)
}

private fun NetworkRule.validateCheckoutAnalyticsRequest(
    eventName: String,
    sessionId: String?,
    vararg requestMatchers: RequestMatcher,
) {
    validateAnalyticsRequest(
        eventName = eventName,
        productUsage = setOf("checkout_elements"),
        sessionId?.let { analyticsPayloadField("checkout_session_id", it) }
            ?: not(hasQueryParam("checkout_session_id")),
        *requestMatchers,
    )
}
