package com.stripe.android.test.core.ui

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

private const val DefaultDelayInMillis = 5_000L
private const val DefaultTimeoutInMillis = 10_000L

internal fun UiDevice.clickTextInWebView(
    label: String,
    delay: Boolean = false,
    selectAmongOccurrences: (List<UiObject2>) -> UiObject2 = { it.single() },
) {
    if (delay) {
        // Some WebView texts are slowly animating in, and UiAutomator seems to struggle
        // with this. These texts can provide this delay to improve the odds.
        Thread.sleep(DefaultDelayInMillis)
    }

    val occurrences = wait(Until.findObjects(By.textContains(label)), DefaultTimeoutInMillis)
    selectAmongOccurrences(occurrences).click()
}
