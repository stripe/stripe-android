package com.stripe.android

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestHardCodedLpms {
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

    private val newUser = TestParameters(
        SupportedPaymentMethod.Bancontact,
        Customer.New,
        GooglePayState.On,
        Currency.EUR,
        Checkout.Pay,
        Billing.Off,
        automatic = Automatic.On,
        delayed = DelayedPMs.Off,
        saveCheckboxValue = false,
        useBrowser = null,
        authorizationAction = AuthorizeAction.Authorize,
        saveForFutureUseCheckboxVisible = false,
        shipping = Shipping.Off
    )

    @Test
    fun testBancontact() = runBlocking {
        testDriver.confirmNewOrGuestCompleteSuccess(
            newUser.copy(
                paymentMethod = SupportedPaymentMethod.Bancontact,
                authorizationAction = AuthorizeAction.Authorize,
            )
        )
    }

    @Test
    fun testSepaDebit() = runBlocking {
        testDriver.confirmNewOrGuestCompleteSuccess(
            newUser.copy(
                paymentMethod = SupportedPaymentMethod.SepaDebit,
                authorizationAction = AuthorizeAction.Authorize,
                automatic = Automatic.Off,
                delayed = DelayedPMs.On,
            )
        ) {
            composeTestRule.onNodeWithText("IBAN")
                .performTextInput("DE89370400440532013000")
        }
    }

    @Test
    fun testIdeal() = runBlocking {
        testDriver.confirmNewOrGuestCompleteSuccess(
            newUser.copy(
                paymentMethod = SupportedPaymentMethod.Ideal,
                authorizationAction = AuthorizeAction.Authorize,
            )
        )
    }

    @Test
    fun testEps() = runBlocking {
        testDriver.confirmNewOrGuestCompleteSuccess(
            newUser.copy(
                paymentMethod = SupportedPaymentMethod.Eps,
                authorizationAction = AuthorizeAction.Authorize,
            )
        )
    }

    @Test
    fun testGiropay() = runBlocking {
        testDriver.confirmNewOrGuestCompleteSuccess(
            newUser.copy(
                paymentMethod = SupportedPaymentMethod.Giropay,
                authorizationAction = AuthorizeAction.Authorize,
            )
        )
    }

    @Test
    fun testP24() = runBlocking {
        testDriver.confirmNewOrGuestCompleteSuccess(
            newUser.copy(
                paymentMethod = SupportedPaymentMethod.P24,
                authorizationAction = AuthorizeAction.Authorize,
            )
        )
    }

    @Test
    fun testAfterpay() = runBlocking {
        testDriver.confirmNewOrGuestCompleteSuccess(
            newUser.copy(
                paymentMethod = SupportedPaymentMethod.AfterpayClearpay,
                authorizationAction = AuthorizeAction.Authorize,
                currency = Currency.USD,
                shipping = Shipping.On
            )
        )
    }

    @Ignore("Complex authorization handling required")
    fun testKlarna() = runBlocking {
        testDriver.confirmNewOrGuestCompleteSuccess(
            newUser.copy(
                paymentMethod = SupportedPaymentMethod.Klarna,
                authorizationAction = AuthorizeAction.Authorize,
                currency = Currency.USD
            )
        )
    }

    @Ignore("Cannot be tested requires EU-based merchant")
    fun testPayPal() = runBlocking {
        testDriver.confirmNewOrGuestCompleteSuccess(
            newUser.copy(
                paymentMethod = SupportedPaymentMethod.PayPal,
                authorizationAction = AuthorizeAction.Authorize,
                currency = Currency.USD
            )
        )
    }
}
