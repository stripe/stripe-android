package com.stripe.android.utils

import androidx.test.platform.app.InstrumentationRegistry
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
                    val command = "am force-stop com.android.chrome"
                    val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
                    uiAutomation.executeShellCommand(command).close()
                }
            }
        }
    }
}
