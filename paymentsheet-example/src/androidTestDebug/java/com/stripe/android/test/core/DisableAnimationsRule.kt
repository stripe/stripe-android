package com.stripe.android.test.core

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
    private val commandsToSetupTestEnvironment = listOf(
        "settings put global transition_animation_scale 0.0",
        "settings put global animator_duration_scale 0.0",
        "settings put global window_animation_scale 0.0",
        "settings put secure long_press_timeout 1500",
        "settings put secure show_ime_with_hard_keyboard 0"
    )

    private fun setDevicePreferences(commands : List<String>) {
        with(device) {
            commands.forEach {
                executeShellCommand(it)
            }
        }
    }

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                setDevicePreferences(commandsToSetupTestEnvironment)
                base.evaluate()
            }
        }
    }
}
