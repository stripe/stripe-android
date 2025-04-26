package com.stripe.android.test.core

import android.os.SystemClock.elapsedRealtime
import android.os.SystemClock.sleep
import androidx.test.espresso.web.sugar.Web.WebInteraction
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.Locator
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Find a button in a web view using its `data-testid` ID (commonly used in Stripe frontend surfaces)
 */
internal fun WebInteraction<Void>.withElementByTestId(
    testId: String,
): WebInteraction<Void> = retryUntil {
    runCatching { withElement(findElement(Locator.CSS_SELECTOR, "[data-testid='$testId']")) }
        .getOrNull()
}

/**
 * Retry until the given block returns a non-null value or the timeout is reached.
 */
private fun <T> retryUntil(
    timeout: Duration = 15.seconds,
    pollInterval: Duration = 3.seconds,
    block: () -> T?
): T {
    val endTime = elapsedRealtime() + timeout.inWholeMilliseconds
    while (elapsedRealtime() < endTime) {
        block()?.let { return it }
        sleep(pollInterval.inWholeMilliseconds)
    }
    throw AssertionError("Condition not met within $timeout")
}
