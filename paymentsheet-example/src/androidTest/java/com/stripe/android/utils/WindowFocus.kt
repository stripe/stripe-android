package com.stripe.android.utils

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice

private const val FOCUS_WAIT_TIMEOUT_MS = 5_000L
private const val FOCUS_POLL_INTERVAL_MS = 100L

/**
 * Polls up to [timeoutMs] for some window to hold input focus, returning true as soon as one does
 * and false on timeout.
 *
 * Returning from a browser/Custom Tab, or force-stopping Chrome, can briefly leave no window
 * focused; the next Espresso interaction then blocks in RootViewPicker for 10s. Callers bound that
 * wait with this. Obtaining [UiDevice] first is required so UiAutomator enables interactive-window
 * retrieval, otherwise [android.app.UiAutomation.getWindows] can return empty.
 */
internal fun awaitWindowFocus(timeoutMs: Long = FOCUS_WAIT_TIMEOUT_MS): Boolean {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    UiDevice.getInstance(instrumentation)
    val uiAutomation = instrumentation.uiAutomation
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
        if (uiAutomation.windows.any { it.isFocused }) {
            return true
        }
        Thread.sleep(FOCUS_POLL_INTERVAL_MS)
    }
    return false
}
