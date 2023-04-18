package com.stripe.android.utils

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * BrowserStack does not offer an API for Espresso tests to disable animations. This rule allows
 * certain tests to disable animations on the device.
 */
class DisableAnimationsRule : TestRule {

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                toggleAnimations(enabled = false)
                try {
                    base.evaluate()
                } finally {
                    toggleAnimations(enabled = true)
                }
            }
        }
    }

    private fun toggleAnimations(enabled: Boolean) {
        val scale = if (enabled) "1.0" else "0.0"

        val commands = listOf(
            "settings put global transition_animation_scale $scale",
            "settings put global animator_duration_scale $scale",
            "settings put global window_animation_scale $scale",
        )

        commands.forEach(device::executeShellCommand)
    }
}
