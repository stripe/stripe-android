package com.stripe.android.test.core

import android.os.SystemClock.elapsedRealtime
import android.os.SystemClock.sleep
import androidx.test.espresso.web.sugar.Web.WebInteraction
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.Locator
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal val WEBVIEW_ELEMENT_TIMEOUT: Duration = 30.seconds

internal class WebElementMatch(
    val testId: String,
    val interaction: WebInteraction<Void>,
)

/**
 * Find a button in a web view using its `data-testid` ID (commonly used in Stripe frontend surfaces).
 *
 * The [timeout] should be generous because the first lookup after a web-based auth activity opens
 * absorbs the full cost of navigating to, bootstrapping, and rendering a remote single-page app whose
 * content only appears after an async data fetch. This mirrors the equally generous activity and
 * idling-resource timeouts elsewhere in the driver and stays well under the 90s per-test timeout.
 */
internal fun WebInteraction<Void>.withElementByTestId(
    testId: String,
    timeout: Duration,
): WebInteraction<Void> = withElementByAnyTestId(
    testIds = listOf(testId),
    timeout = timeout,
).interaction

/**
 * Finds the first available element from [testIds] without spending the full timeout on a selector for a
 * different server-controlled flow variant.
 */
internal fun WebInteraction<Void>.withElementByAnyTestId(
    testIds: List<String>,
    timeout: Duration,
): WebElementMatch {
    require(testIds.isNotEmpty()) { "testIds must not be empty" }

    var lastFailure: Throwable? = null
    return retryUntil(
        timeout = timeout,
        pollInterval = 1.seconds,
        failureMessage = "No WebView element found with data-testid in " +
            testIds.joinToString(prefix = "[", postfix = "]") { "'$it'" },
        failureCause = { lastFailure },
    ) {
        testIds.firstNotNullOfOrNull { testId ->
            runCatching {
                WebElementMatch(
                    testId = testId,
                    interaction = withElement(findElement(Locator.CSS_SELECTOR, "[data-testid='$testId']")),
                )
            }.onFailure {
                lastFailure = it
            }.getOrNull()
        }
    }
}

/**
 * Retry until the given block returns a non-null value or the timeout is reached. Polls frequently so
 * the target is detected promptly once it renders rather than up to a full [pollInterval] later.
 */
private fun <T> retryUntil(
    timeout: Duration,
    pollInterval: Duration,
    failureMessage: String,
    failureCause: () -> Throwable?,
    block: () -> T?,
): T {
    val endTime = elapsedRealtime() + timeout.inWholeMilliseconds
    while (elapsedRealtime() < endTime) {
        block()?.let { return it }
        sleep(pollInterval.inWholeMilliseconds)
    }

    val error = AssertionError("$failureMessage within $timeout")
    failureCause()?.let(error::initCause)
    throw error
}
