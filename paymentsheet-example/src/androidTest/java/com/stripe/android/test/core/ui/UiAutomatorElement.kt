package com.stripe.android.test.core.ui

import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat

internal class UiAutomatorElement(
    private val bySelector: BySelector,
    private val device: UiDevice,
    private val elementFinder: (UiObject2) -> UiObject2? = { it }
) {
    fun click() {
        waitForExists()
        val uiObject = elementFinder(device.findObject(bySelector))
        assertThat(uiObject).isNotNull()
        uiObject?.click()
    }

    private fun waitForExists(timeoutInMillis: Long = 5_000L) {
        assertThat(device.wait(Until.hasObject(bySelector), timeoutInMillis)).isNotNull()
    }

    fun setText(text: String) {
        waitForExists()
        val uiObject = elementFinder(device.findObject(bySelector))
        assertThat(uiObject).isNotNull()
        uiObject?.text = text
    }
}
