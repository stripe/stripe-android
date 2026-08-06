package com.stripe.android.utils

import android.app.UiAutomation
import android.os.Build
import android.os.ParcelFileDescriptor
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
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                val device = UiDevice.getInstance(instrumentation)

                // Stabilize Chrome startup on the API 33 emulator by skipping its first-run
                // prompts and Vulkan path. Keep GPU compositing enabled because --disable-gpu
                // causes this Chrome image to abort before opening the authorization page.
                configureChrome(instrumentation.uiAutomation)

                try {
                    base.evaluate()
                } finally {
                    val command = "am force-stop com.android.chrome"
                    instrumentation.uiAutomation.executeShellCommand(command).close()

                    // Force-stopping Chrome leaves no window focused; restore focus so the next
                    // test's Espresso RootViewPicker doesn't time out waiting for it.
                    device.wakeUp()
                    device.pressHome()
                    awaitWindowFocus()
                }
            }
        }
    }

    private fun configureChrome(uiAutomation: UiAutomation) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return
        }

        val descriptors = uiAutomation.executeShellCommandRw("sh")
        ParcelFileDescriptor.AutoCloseOutputStream(descriptors[1]).bufferedWriter().use { writer ->
            writer.write(
                "echo chrome --disable-fre --no-default-browser-check " +
                    "--disable-features=Vulkan " +
                    "> /data/local/tmp/chrome-command-line"
            )
            writer.newLine()
        }
        ParcelFileDescriptor.AutoCloseInputStream(descriptors[0]).use { it.readBytes() }
    }
}
