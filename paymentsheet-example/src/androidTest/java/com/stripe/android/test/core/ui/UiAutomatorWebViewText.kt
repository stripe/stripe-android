package com.stripe.android.test.core.ui

import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

private const val DefaultTimeoutInMillis = 10_000L

open class UiAutomatorWebViewText(
    private val label: String,
    private val device: UiDevice,
) {

    fun click(
        selectAmongOccurrences: (List<UiObject2>) -> UiObject2 = { it.single() },
    ) {
        val occurrences = device.wait(Until.findObjects(selector()), DefaultTimeoutInMillis)
        selectAmongOccurrences(occurrences).click()
    }

    private fun selector(): BySelector {
        return By.textContains(label)
    }
}
