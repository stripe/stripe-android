package com.stripe.android.test.core.ui

import androidx.test.uiautomator.UiDevice
import com.stripe.android.test.core.DEFAULT_UI_TIMEOUT

class UiAutomatorWindow(
    private val device: UiDevice,
    private val packageName: String
) {
    fun waitFor() {
        device.waitForWindowUpdate(packageName, DEFAULT_UI_TIMEOUT.inWholeMilliseconds)
    }
}
