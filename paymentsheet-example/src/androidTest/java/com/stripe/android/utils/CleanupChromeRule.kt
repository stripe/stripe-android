package com.stripe.android.utils

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

// Kills Chrome once the test is over.
// This is to prevent the Chrome process from causing future tests to fail.
internal object CleanupChromeRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            @Throws
            override fun evaluate() {
                try {
                    base.evaluate()
                } finally {
                    val instrumentation = InstrumentationRegistry.getInstrumentation()
                    val command = "am force-stop com.android.chrome"
                    instrumentation.uiAutomation.executeShellCommand(command).close()

                    // Force-stopping Chrome leaves no window focused; restore focus so the next
                    // test's Espresso RootViewPicker doesn't time out waiting for it.
                    val device = UiDevice.getInstance(instrumentation)
                    device.wakeUp()
                    device.pressHome()
                    awaitWindowFocus()
                }
            }
        }
    }
}
