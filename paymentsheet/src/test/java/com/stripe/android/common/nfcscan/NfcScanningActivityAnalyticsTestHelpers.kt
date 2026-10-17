package com.stripe.android.common.nfcscan

import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatcher
import com.stripe.android.networktesting.RequestMatchers.analyticsPayloadField
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method

private const val ANALYTICS_HOST = "q.stripe.com"
private const val EVENT_PREFIX = "mc_"

internal fun NetworkRule.expectNfcScanStarted() {
    expectAnalyticsEvent("${EVENT_PREFIX}nfc_scan_started")
}

internal fun NetworkRule.expectNfcScanCanceled() {
    expectAnalyticsEvent("${EVENT_PREFIX}nfc_scan_canceled")
}

internal fun NetworkRule.expectNfcScanAttemptStarted() {
    expectAnalyticsEvent("${EVENT_PREFIX}nfc_scan_attempt_started")
}

internal fun NetworkRule.expectNfcScanAttemptSucceeded() {
    expectAnalyticsEvent("${EVENT_PREFIX}nfc_scan_attempt_succeeded")
}

internal fun NetworkRule.expectNfcScanSuccess() {
    expectAnalyticsEvent("${EVENT_PREFIX}nfc_scan_success")
}

internal fun NetworkRule.expectNfcScanAttemptFailed(
    errorCode: String,
    errorMatchers: List<RequestMatcher> = emptyList(),
) {
    expectAnalyticsEvent(
        eventName = "${EVENT_PREFIX}nfc_scan_attempt_failed",
        matchers = listOf(
            analyticsPayloadField("error_code", errorCode),
        ) + errorMatchers,
    )
}

internal fun createApduErrorMatchers(
    executedCommands: List<String>,
    sw1: String,
    sw2: String
): List<RequestMatcher> {
    return listOf(
        analyticsPayloadField("sw1", sw1),
        analyticsPayloadField("sw2", sw2),
        RequestMatcher { request ->
            request.queryParameterValues("executed_commands[]") == executedCommands
        },
    )
}

private fun NetworkRule.expectAnalyticsEvent(
    eventName: String,
    matchers: List<RequestMatcher> = emptyList(),
) {
    val matchers = buildList {
        add(host(ANALYTICS_HOST))
        add(method("GET"))
        add(analyticsPayloadField("event", eventName))
        addAll(matchers)
    }

    enqueue(*matchers.toTypedArray()) { response ->
        response.status = "HTTP/1.1 200 OK"
    }
}
