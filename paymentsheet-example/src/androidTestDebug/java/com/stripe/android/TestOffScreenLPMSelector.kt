package com.stripe.android

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.test.core.Automatic
import com.stripe.android.test.core.Billing
import com.stripe.android.test.core.Browser
import com.stripe.android.test.core.Currency
import com.stripe.android.test.core.Customer
import com.stripe.android.test.core.DelayedPMs
import com.stripe.android.test.core.GooglePayState
import com.stripe.android.test.core.INDIVIDUAL_TEST_TIMEOUT_SECONDS
import com.stripe.android.test.core.IntentType
import com.stripe.android.test.core.MyScreenCaptureProcessor
import com.stripe.android.test.core.PlaygroundTestDriver
import com.stripe.android.test.core.Shipping
import com.stripe.android.test.core.TEST_IBAN_NUMBER
import com.stripe.android.test.core.TestParameters
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestOffScreenLPMSelector {
    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(INDIVIDUAL_TEST_TIMEOUT_SECONDS)

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private lateinit var device: UiDevice
    private lateinit var testDriver: PlaygroundTestDriver
    private val screenshotProcessor = MyScreenCaptureProcessor()

    @Before
    fun before() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        testDriver = PlaygroundTestDriver(device, composeTestRule, screenshotProcessor)
    }

    @After
    fun after() {
    }

    @Test
    fun scrollToLpmAndCustomFields() {
        testDriver.confirmNewOrGuestComplete(
            TestParameters(
                SupportedPaymentMethod.SepaDebit,
                Customer.New,
                GooglePayState.On,
                Currency.EUR,
                IntentType.Pay,
                Billing.On,
                shipping = Shipping.Off,
                delayed = DelayedPMs.On,
                automatic = Automatic.Off,
                saveCheckboxValue = false,
                saveForFutureUseCheckboxVisible = false,
                useBrowser = Browser.Chrome,
                authorizationAction = null,
            )
        ) {
            composeTestRule.onNodeWithText("IBAN")
                .performTextInput(TEST_IBAN_NUMBER)
        }
    }
}
