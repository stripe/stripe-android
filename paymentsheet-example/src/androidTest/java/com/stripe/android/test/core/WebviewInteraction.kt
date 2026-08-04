package com.stripe.android.test.core

import android.os.SystemClock.elapsedRealtime
import android.os.SystemClock.sleep
import androidx.test.espresso.web.sugar.Web.WebInteraction
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.Locator
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Find a button in a web view using its `data-testid` ID (commonly used in Stripe frontend surfaces).
 *
 * The default [timeout] is generous because the first lookup after a web-based auth activity opens
 * absorbs the full cost of navigating to, bootstrapping, and rendering a remote single-page app whose
 * content only appears after an async data fetch. This mirrors the equally generous activity and
 * idling-resource timeouts elsewhere in the driver and stays well under the 90s per-test timeout.
 */
internal fun WebInteraction<Void>.withElementByTestId(
    testId: String,
    timeout: Duration = 30.seconds,
): WebInteraction<Void> = retryUntil(timeout = timeout) {
    runCatching { withElement(findElement(Locator.CSS_SELECTOR, "[data-testid='$testId']")) }
        .getOrNull()
}

/**
 * Retry until the given block returns a non-null value or the timeout is reached. Polls frequently so
 * the target is detected promptly once it renders rather than up to a full [pollInterval] later.
 */
private fun <T> retryUntil(
    timeout: Duration = 30.seconds,
    pollInterval: Duration = 1.seconds,
    block: () -> T?
): T {
    val endTime = elapsedRealtime() + timeout.inWholeMilliseconds
    while (elapsedRealtime() < endTime) {
        block()?.let { return it }
        sleep(pollInterval.inWholeMilliseconds)
    }
    throw AssertionError("Condition not met within $timeout")
}
