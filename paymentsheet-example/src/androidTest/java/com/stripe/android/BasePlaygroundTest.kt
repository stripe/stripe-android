package com.stripe.android

import androidx.test.espresso.intent.Intents
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.stripe.android.test.core.PlaygroundTestDriver
import com.stripe.android.utils.TestRules
import leakcanary.LeakCanary
import org.junit.After
import org.junit.Before
import org.junit.Rule
import shark.LibraryLeakReferenceMatcher
import shark.ReferencePattern

internal open class BasePlaygroundTest(disableAnimations: Boolean = true) {
    @get:Rule
    val rules = TestRules.create(disableAnimations = disableAnimations)

    lateinit var device: UiDevice
    lateinit var testDriver: PlaygroundTestDriver

    init {
        LeakCanary.config = LeakCanary.config.copy(
            referenceMatchers = LeakCanary.config.referenceMatchers +
                LibraryLeakReferenceMatcher(ReferencePattern.JavaLocalPattern("hh.a"))
        )
    }

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
