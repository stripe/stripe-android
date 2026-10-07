package com.stripe.android.testing

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Isolates browser state for tests that exercise a real backend and external authentication UI. */
class ExternalUiTestRule : TestWatcher() {
    private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    override fun starting(description: Description) {
        device.wakeUp()
        device.executeShellCommand("pm clear com.android.chrome")
        device.executeShellCommand(
            "sh -c 'echo chrome --disable-fre --no-default-browser-check " +
                "--disable-features=Vulkan > /data/local/tmp/chrome-command-line'"
        )
    }

    override fun finished(description: Description) {
        device.executeShellCommand("am force-stop com.android.chrome")
        device.pressHome()
    }
}
