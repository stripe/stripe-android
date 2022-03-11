package com.stripe.android

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestPlatformInfrastructureTest {

    @Rule
    var globalTimeout: Timeout = Timeout.seconds(INDIVIDUAL_TEST_TIMEOUT_SECONDS)

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private lateinit var device: UiDevice
    private lateinit var testDriver: PlaygroundTestDriver
    private val screenshotProcessor = MyScreenCaptureProcessor()

    @Before
    fun before() {
        androidx.test.espresso.intent.Intents.init()
        device = UiDevice.getInstance(getInstrumentation())
        testDriver = PlaygroundTestDriver(device, composeTestRule, screenshotProcessor)
    }

    @After
    fun after() {
        androidx.test.espresso.intent.Intents.release()
    }


    // TODO: Verify smart browser handling
    // TODO: Handle authorize failure.
    // TODO: Handle authorize success.
    // TODO: Add in all existing LPMs: Klarna, Afterpay, Eps, Bancontact, Sepa, P24, Sofort, iDeal, giropay
    // TODO: Playground looks a little funny with the buttons we can scroll to the buy button now
    // TODO: Test with setup intents as well.
    // TODO: Need a card test when compose is in.
    // TODO: Dropdown
    // TODO: Localize address fields
}
