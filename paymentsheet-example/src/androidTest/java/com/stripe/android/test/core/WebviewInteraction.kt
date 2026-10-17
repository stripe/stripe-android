package com.stripe.android.test.core

import android.os.SystemClock.elapsedRealtime
import android.os.SystemClock.sleep
import androidx.test.espresso.web.model.Atoms.script
import androidx.test.espresso.web.sugar.Web.WebInteraction
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.Locator
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal val WEBVIEW_ELEMENT_TIMEOUT: Duration = 30.seconds

private const val RENDERED_TEST_ID_PREFIX = "testId="

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

    var lastProbe: String? = null
    var lastFailure: Throwable? = null
    return retryUntil(
        timeout = timeout,
        pollInterval = 1.seconds,
        failureMessage = {
            "No WebView element found with data-testid in " +
                testIds.joinToString(prefix = "[", postfix = "]") { "'$it'" } +
                " within $timeout; last probe: ${lastProbe ?: "none"}"
        },
        failureCause = { lastFailure },
    ) {
        runCatching {
            val probe = probePage(testIds).also { lastProbe = it }
            probe.takeIf { it.startsWith(RENDERED_TEST_ID_PREFIX) }
                ?.removePrefix(RENDERED_TEST_ID_PREFIX)
                ?.let { testId ->
                    WebElementMatch(
                        testId = testId,
                        interaction = withElement(findElement(Locator.CSS_SELECTOR, cssSelector(testId))),
                    )
                }
        }.onFailure { lastFailure = it }.getOrNull()
    }
}

/**
 * Reports the first of [testIds] the page currently renders, or, while none of them is present, what the
 * page is showing instead. Answering both in one script evaluation keeps a poll that finds nothing off the
 * exception path: probing with [findElement] signals absence by throwing, which routes every poll taken
 * before the pane renders through Espresso's failure handler, and that handler captures a screenshot per
 * failure, costing seconds of the timeout budget per selector per poll on a loaded device. The reported
 * page also lets a lookup failure distinguish a page that never loaded from a loaded page that renders
 * none of the expected controls, without a second interaction once the test is already failing.
 *
 * The script has to be a function definition rather than a bare statement body. Espresso inlines a
 * function definition into the page, but hands a statement body to `eval`, which the hosted auth page's
 * Content-Security-Policy rejects for lacking `unsafe-eval`.
 */
private fun WebInteraction<Void>.probePage(testIds: List<String>): String {
    val probeScript = testIds.joinToString(
        separator = "\n",
        prefix = "function() {\n",
        postfix = "\nreturn 'url=' + document.location.href + ' readyState=' + document.readyState;\n}",
    ) { testId ->
        """if (document.querySelector("${cssSelector(testId)}") !== null) { """ +
            """return "$RENDERED_TEST_ID_PREFIX$testId"; }"""
    }

    return perform(script(probeScript)).get().value?.toString() ?: "no probe result"
}

private fun cssSelector(testId: String): String = "[data-testid='$testId']"

/**
 * Retry until the given block returns a non-null value or the timeout is reached. Polls frequently so
 * the target is detected promptly once it renders rather than up to a full [pollInterval] later.
 */
private fun <T> retryUntil(
    timeout: Duration,
    pollInterval: Duration,
    failureMessage: () -> String,
    failureCause: () -> Throwable?,
    block: () -> T?,
): T {
    val endTime = elapsedRealtime() + timeout.inWholeMilliseconds
    while (elapsedRealtime() < endTime) {
        block()?.let { return it }
        sleep(pollInterval.inWholeMilliseconds)
    }

    val error = AssertionError(failureMessage())
    failureCause()?.let(error::initCause)
    throw error
}
