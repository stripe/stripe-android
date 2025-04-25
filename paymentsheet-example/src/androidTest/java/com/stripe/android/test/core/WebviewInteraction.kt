package com.stripe.android.test.core

import android.os.SystemClock.elapsedRealtime
import android.os.SystemClock.sleep
import androidx.test.espresso.web.sugar.Web.WebInteraction
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.Locator
import java.util.concurrent.TimeUnit

/**
 * Find a button in a web view using its `data-testid` ID (commonly used in Stripe frontend surfaces)
 */
internal fun WebInteraction<Void>.withElementByTestId(
    testId: String,
): WebInteraction<Void> = retryUntil {
    runCatching { withElement(findElement(Locator.CSS_SELECTOR, "[data-testid='$testId']")) }
        .getOrNull()
}

private fun <T> retryUntil(
    timeoutSeconds: Long = 15,
    pollIntervalMillis: Long = 3,
    block: () -> T?
): T {
    val endTime = elapsedRealtime() + TimeUnit.SECONDS.toMillis(timeoutSeconds)
    while (elapsedRealtime() < endTime) {
        val result = block()
        if (result != null) {
            return result
        }
        sleep(pollIntervalMillis)
    }
    throw AssertionError("Condition not met within $timeoutSeconds seconds")
}
