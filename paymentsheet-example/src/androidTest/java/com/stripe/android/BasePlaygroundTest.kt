package com.stripe.android

import androidx.test.espresso.intent.Intents
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.stripe.android.test.core.PlaygroundTestDriver
import com.stripe.android.utils.TestRules
import org.junit.After
import org.junit.Before
import org.junit.Rule

internal open class BasePlaygroundTest(
    disableAnimations: Boolean = true,
    retryEnabled: Boolean = false,
    retryCount: Int = 3,
) {
    @get:Rule
    val rules = TestRules.create(
        disableAnimations = disableAnimations,
        retryEnabled = retryEnabled,
        retryCount = retryCount,
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
        Intents.release()
    }
}
