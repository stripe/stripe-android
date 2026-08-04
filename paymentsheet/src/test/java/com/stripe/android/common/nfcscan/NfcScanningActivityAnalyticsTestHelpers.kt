package com.stripe.android.common.nfcscan

import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.host
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.query

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
) {
    expectAnalyticsEvent(
        eventName = "${EVENT_PREFIX}nfc_scan_attempt_failed",
        errorCode = errorCode,
    )
}

private fun NetworkRule.expectAnalyticsEvent(
    eventName: String,
    errorCode: String? = null,
) {
    val matchers = buildList {
        add(host(ANALYTICS_HOST))
        add(method("GET"))
        add(query("event", eventName))
        if (errorCode != null) {
            add(query("error_code", errorCode))
        }
    }

    enqueue(*matchers.toTypedArray()) { response ->
        response.status = "HTTP/1.1 200 OK"
    }
}
