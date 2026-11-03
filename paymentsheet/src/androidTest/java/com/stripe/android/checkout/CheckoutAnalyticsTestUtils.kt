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

/**
 * Validates an operation reported as a single event with no started and succeeded or failed pair, such as
 * clear_payment_option.
 */
internal fun NetworkRule.validateCheckoutOperationEvent(
    operation: String,
    sessionId: String?,
    vararg requestMatchers: RequestMatcher,
) {
    validateCheckoutOperationAnalyticsRequest(
        "checkout_elements.$operation",
        sessionId,
        not(hasQueryParam("duration")),
        *requestMatchers,
    )
}

/**
 * Validates the started event of an update_shipping_address or update_billing_address operation.
 * did_request_tax_update is reported only with succeeded or failed, so it must be absent here.
 */
internal fun NetworkRule.validateCheckoutAddressUpdateStarted(
    operation: String,
    invocationSource: String,
    sessionId: String? = DEFAULT_CHECKOUT_SESSION_ID,
    vararg requestMatchers: RequestMatcher,
) {
    validateCheckoutOperationStarted(
        operation,
        sessionId,
        analyticsPayloadField("invocation_source", invocationSource),
        not(hasQueryParam("did_request_tax_update")),
        *requestMatchers,
    )
}

/**
 * Validates the succeeded or failed event of an update_shipping_address or update_billing_address operation.
 * Every succeeded or failed event carries did_request_tax_update: false unless a tax update request was sent,
 * including operations rejected before they did any work.
 */
internal fun NetworkRule.validateCheckoutAddressUpdateResult(
    operation: String,
    outcome: String,
    invocationSource: String,
    didRequestTaxUpdate: Boolean,
    sessionId: String? = DEFAULT_CHECKOUT_SESSION_ID,
    vararg requestMatchers: RequestMatcher,
) {
    validateCheckoutOperationResult(
        "checkout_elements.$operation.$outcome",
        sessionId,
        analyticsPayloadField("invocation_source", invocationSource),
        analyticsPayloadField("did_request_tax_update", didRequestTaxUpdate.toString()),
        *requestMatchers,
    )
}

/**
 * Validates started and succeeded events of an update_shipping_address or update_billing_address
 * operation. Both events carry the invocation source; only the succeeded event carries
 * did_request_tax_update. [requestMatchers] apply to both events.
 */
internal fun NetworkRule.validateCheckoutAddressUpdate(
    operation: String,
    invocationSource: String,
    didRequestTaxUpdate: Boolean,
    sessionId: String? = DEFAULT_CHECKOUT_SESSION_ID,
    vararg requestMatchers: RequestMatcher,
) {
    validateCheckoutAddressUpdateStarted(operation, invocationSource, sessionId, *requestMatchers)
    validateCheckoutAddressUpdateResult(
        operation,
        "succeeded",
        invocationSource,
        didRequestTaxUpdate,
        sessionId,
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
