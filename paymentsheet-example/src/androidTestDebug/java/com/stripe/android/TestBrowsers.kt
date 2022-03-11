package com.stripe.android

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestBrowsers {
    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(INDIVIDUAL_TEST_TIMEOUT_SECONDS)
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private lateinit var device: UiDevice
    private lateinit var testDriver: PlaygroundTestDriver
    private val screenshotProcessor = MyScreenCaptureProcessor()

    @Before
    fun before() {
        androidx.test.espresso.intent.Intents.init()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        testDriver = PlaygroundTestDriver(device, composeTestRule, screenshotProcessor)
    }

    @After
    fun after() {
        androidx.test.espresso.intent.Intents.release()
    }

    private val bancontactNewUser = TestParameters(
        SupportedPaymentMethod.Bancontact,
        Customer.New,
        GooglePayState.On,
        Currency.EUR,
        Checkout.Pay,
        Billing.Off,
        automatic = Automatic.On,
        delayed = DelayedPMs.Off,
        saveCheckboxValue = false,
        useBrowser = Browser.Chrome,
        authorizationAction = AuthorizeAction.Authorize(),
        saveForFutureUseCheckboxVisible = false,
        shipping = Shipping.Off
    )

    @Test
    fun testAuthorizeChrome() = runBlocking {
        testDriver.confirmNewOrGuestCompleteSuccess(
            bancontactNewUser.copy(
                useBrowser = Browser.Chrome,
            )
        )
    }

    @Test
    fun testAuthorizeFirefox() = runBlocking {
        testDriver.confirmNewOrGuestCompleteSuccess(
            bancontactNewUser.copy(
                useBrowser = Browser.Firefox,
            )
        )
    }

    @Test
    fun testAuthorizeAndroidBrowser() = runBlocking {
        // Does not work when default browser is android
        testDriver.confirmNewOrGuestCompleteSuccess(
            bancontactNewUser.copy(
                useBrowser = Browser.Android,
            )
        )
    }

    @Test
    fun testAuthorizeAnyAvailableBrowser() = runBlocking {
        // Does not work when default browser is android
        testDriver.confirmNewOrGuestCompleteSuccess(
            bancontactNewUser.copy(
                useBrowser = null,
            )
        )
    }
}
