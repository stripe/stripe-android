package com.stripe.android

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.IdlingPolicies
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.Automatic
import com.stripe.android.test.core.Billing
import com.stripe.android.test.core.IntentType
import com.stripe.android.test.core.Currency
import com.stripe.android.test.core.Customer
import com.stripe.android.test.core.DelayedPMs
import com.stripe.android.test.core.GooglePayState
import com.stripe.android.test.core.INDIVIDUAL_TEST_TIMEOUT_SECONDS
import com.stripe.android.test.core.MyScreenCaptureProcessor
import com.stripe.android.test.core.PlaygroundTestDriver
import com.stripe.android.test.core.Shipping
import com.stripe.android.test.core.TestParameters
import com.stripe.android.test.core.TestWatcher
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TestHardCodedLpms {
    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(INDIVIDUAL_TEST_TIMEOUT_SECONDS)

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @get:Rule
    val testWatcher = TestWatcher()

    private lateinit var device: UiDevice
    private lateinit var testDriver: PlaygroundTestDriver

    companion object {
        private val screenshotProcessor = MyScreenCaptureProcessor()
    }

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

    private val newUser = TestParameters(
        SupportedPaymentMethod.Bancontact,
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
        useBrowser = null,
        authorizationAction = AuthorizeAction.Authorize,
        takeScreenshotOnLpmLoad = true
    )

    @Test
    fun testBancontact()  {
        testDriver.confirmNewOrGuestComplete(
            newUser.copy(
                paymentMethod = SupportedPaymentMethod.Bancontact,
                authorizationAction = AuthorizeAction.Authorize,
            )
        )
    }

    @Test
    fun testSepaDebit()  {
        testDriver.confirmNewOrGuestComplete(
            newUser.copy(
                paymentMethod = SupportedPaymentMethod.SepaDebit,
                authorizationAction = null,
                automatic = Automatic.Off,
                delayed = DelayedPMs.On,
            )
        ) {
            composeTestRule.onNodeWithText("IBAN")
                .performTextInput("DE89370400440532013000")
        }
    }

    @Test
    fun testIdeal()  {
        testDriver.confirmNewOrGuestComplete(
            newUser.copy(
                paymentMethod = SupportedPaymentMethod.Ideal,
                authorizationAction = AuthorizeAction.Authorize,
            )
        )
    }

    @Test
    fun testEps()  {
        testDriver.confirmNewOrGuestComplete(
            newUser.copy(
                paymentMethod = SupportedPaymentMethod.Eps,
                authorizationAction = AuthorizeAction.Authorize,
            )
        )
    }

    @Test
    fun testGiropay()  {
        testDriver.confirmNewOrGuestComplete(
            newUser.copy(
                paymentMethod = SupportedPaymentMethod.Giropay,
                authorizationAction = AuthorizeAction.Authorize,
            )
        )
    }

    @Test
    fun testP24() {
        testDriver.confirmNewOrGuestComplete(
            newUser.copy(
                paymentMethod = SupportedPaymentMethod.P24,
                authorizationAction = AuthorizeAction.Authorize,
            )
        )
    }

    @Test
    fun testAfterpay()  {
        testDriver.confirmNewOrGuestComplete(
            newUser.copy(
                paymentMethod = SupportedPaymentMethod.AfterpayClearpay,
                authorizationAction = AuthorizeAction.Authorize,
                currency = Currency.USD,
                shipping = Shipping.On
            )
        )
    }

    @Ignore("Complex authorization handling required")
    fun testKlarna()  {
        testDriver.confirmNewOrGuestComplete(
            newUser.copy(
                paymentMethod = SupportedPaymentMethod.Klarna,
                authorizationAction = AuthorizeAction.Authorize,
                currency = Currency.USD
            )
        )
    }

    @Ignore("Cannot be tested requires EU-based merchant")
    fun testPayPal()  {
        testDriver.confirmNewOrGuestComplete(
            newUser.copy(
                paymentMethod = SupportedPaymentMethod.PayPal,
                authorizationAction = AuthorizeAction.Authorize,
                currency = Currency.USD
            )
        )
    }
}
