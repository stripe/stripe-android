package com.stripe.android.checkout

import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.analyticsPayloadField
import com.stripe.android.networktesting.RequestMatchers.hasQueryParam
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.paymentsheet.validateAnalyticsRequest

internal fun NetworkRule.validateCheckoutInitStarted(
    sessionId: String,
    vararg requestMatchers: RequestMatcher,
) {
    validateCheckoutOperationStarted("init", sessionId, *requestMatchers)
}

internal fun NetworkRule.validateCheckoutInitSucceeded(
    sessionId: String,
    vararg requestMatchers: RequestMatcher,
) {
    validateCheckoutOperationResult("checkout_elements.init.succeeded", sessionId, *requestMatchers)
}

internal fun NetworkRule.validateCheckoutInitFailed(
    sessionId: String,
    vararg requestMatchers: RequestMatcher,
) {
    validateCheckoutOperationResult("checkout_elements.init.failed", sessionId, *requestMatchers)
}

/**
 * Validates a started event for a Checkout operation. A null [sessionId] expects no session identity.
 */
internal fun NetworkRule.validateCheckoutOperationStarted(
    operation: String,
    sessionId: String?,
    vararg requestMatchers: RequestMatcher,
) {
    validateCheckoutOperationAnalyticsRequest(
        "checkout_elements.$operation.started",
        sessionId,
        not(hasQueryParam("duration")),
        *requestMatchers,
    )
}

/**
 * Validates a Checkout operation's succeeded or failed event.
 */
internal fun NetworkRule.validateCheckoutOperationResult(
    eventName: String,
    sessionId: String?,
    vararg requestMatchers: RequestMatcher,
) {
    validateCheckoutOperationAnalyticsRequest(
        eventName,
        sessionId,
        hasQueryParam("duration"),
        *requestMatchers,
    )
}

internal fun NetworkRule.validateConfirmationStartedAnalyticsRequest(
    invocationSource: String,
    selectedLpm: String,
    vararg requestMatchers: RequestMatcher,
) {
    validateConfirmationAnalyticsRequest(
        "checkout_elements.confirm.started",
        invocationSource,
        selectedLpm,
        not(hasQueryParam("duration")),
        *requestMatchers,
    )
}

/**
 * Validates a confirm event's outcome: succeeded, failed, or canceled.
 */
internal fun NetworkRule.validateConfirmationResultAnalyticsRequest(
    eventName: String,
    invocationSource: String,
    selectedLpm: String,
    vararg requestMatchers: RequestMatcher,
) {
    validateConfirmationAnalyticsRequest(
        eventName,
        invocationSource,
        selectedLpm,
        hasQueryParam("duration"),
        *requestMatchers,
    )
}

private fun NetworkRule.validateConfirmationAnalyticsRequest(
    eventName: String,
    invocationSource: String,
    selectedLpm: String,
    vararg requestMatchers: RequestMatcher,
) {
    validateAnalyticsRequest(
        eventName = eventName,
        productUsage = setOf("checkout_elements"),
        analyticsPayloadField("checkout_session_id", DEFAULT_CHECKOUT_SESSION_ID),
        analyticsPayloadField("invocation_source", invocationSource),
        analyticsPayloadField("selected_lpm", selectedLpm),
        *requestMatchers,
    )
}

private fun NetworkRule.validateCheckoutOperationAnalyticsRequest(
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
