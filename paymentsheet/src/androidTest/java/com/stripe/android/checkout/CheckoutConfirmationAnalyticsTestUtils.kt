package com.stripe.android.checkout

import com.stripe.android.checkouttesting.DEFAULT_CHECKOUT_SESSION_ID
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.analyticsPayloadField
import com.stripe.android.networktesting.RequestMatchers.hasQueryParam
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.paymentsheet.validateAnalyticsRequest

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
 * Validates a terminal confirm event: succeeded, failed, or canceled.
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
