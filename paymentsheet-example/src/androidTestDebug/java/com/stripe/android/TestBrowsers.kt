package com.stripe.android

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.stripe.android.test.core.AuthorizeAction
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
import com.stripe.android.test.core.TestParameters
import com.stripe.android.test.core.TestWatcher
import com.stripe.android.ui.core.forms.resources.LpmRepository
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * This tests that authorization works with firefox and chrome browsers.  If a browser
 * is specified in the test parameters and it is not available the test will be skipped.
 * Note that if a webview is used because there is no browser or a returnURL is specified
 * this it cannot be actuated with UI Automator.
 */
@RunWith(AndroidJUnit4::class)
class TestBrowsers {
    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(INDIVIDUAL_TEST_TIMEOUT_SECONDS)

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @get:Rule
    val testWatcher = TestWatcher()

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

    private val bancontactNewUser = TestParameters(
        lpmRepository.fromCode("bancontact")!!,
        Customer.New,
        GooglePayState.On,
        Currency.EUR,
        IntentType.Pay,
        Billing.On,
        shipping = Shipping.Off,
        delayed = DelayedPMs.Off,
        automatic = Automatic.On,
        saveCheckboxValue = false,
        saveForFutureUseCheckboxVisible = false,
        useBrowser = Browser.Chrome,
        authorizationAction = AuthorizeAction.Authorize,
        merchantCountryCode = "GB",
    )

    @Test
    fun testAuthorizeChrome() {
        testDriver.confirmNewOrGuestComplete(
            bancontactNewUser.copy(
                useBrowser = Browser.Chrome,
            )
        )
    }

    @Ignore("On browserstack's Google Pixel, the connection to stripe.com is deemed insecure and the page does not load.")
    fun testAuthorizeFirefox() {
        testDriver.confirmNewOrGuestComplete(
            bancontactNewUser.copy(
                useBrowser = Browser.Firefox,
            )
        )
    }

    @Test
    fun testAuthorizeAnyAvailableBrowser() {
        // Does not work when default browser is android
        testDriver.confirmNewOrGuestComplete(
            bancontactNewUser.copy(
                useBrowser = null,
            )
        )
    }

    companion object {
        private val lpmRepository = LpmRepository(
            LpmRepository.LpmRepositoryArguments(
                InstrumentationRegistry.getInstrumentation().targetContext.resources
            )
        ).apply {
            forceUpdate(LpmRepository.exposedPaymentMethods, null)
        }
    }
}
