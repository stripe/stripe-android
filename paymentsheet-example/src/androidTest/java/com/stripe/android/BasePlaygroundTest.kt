package com.stripe.android

import androidx.test.espresso.intent.Intents
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.stripe.android.test.core.PlaygroundTestDriver
import com.stripe.android.utils.TestRules
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.RuleChain

internal open class BasePlaygroundTest(
    disableAnimations: Boolean = true,
    block: RuleChain.() -> RuleChain = { this },
) {
    @get:Rule
    open val rules = TestRules.create(
        disableAnimations = disableAnimations,
        block = block,
    )

    lateinit var device: UiDevice
    lateinit var testDriver: PlaygroundTestDriver

    @Before
    fun before() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        testDriver = PlaygroundTestDriver(device, rules.compose)
    }

    @After
    fun after() {
        // Runs even if the test body threw before its own teardown() call, so a destroyed
        // activity is never left referenced by the driver.
        testDriver.teardown()
        Intents.release()
    }
}
