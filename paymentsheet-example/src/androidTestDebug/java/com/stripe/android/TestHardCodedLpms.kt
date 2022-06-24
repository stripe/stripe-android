package com.stripe.android

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.Automatic
import com.stripe.android.test.core.Billing
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

    @get:Rule
    val testWatcher = TestWatcher()

    private lateinit var device: UiDevice
    private lateinit var testDriver: PlaygroundTestDriver

    companion object {
        // There exists only one screenshot processor so that all tests put
        // their files in the same directory.
        private val screenshotProcessor = MyScreenCaptureProcessor()

        private val lpmRepository = LpmRepository(
            InstrumentationRegistry.getInstrumentation().targetContext.resources
        )
    }

    @Before
    fun before() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        testDriver = PlaygroundTestDriver(device, composeTestRule, screenshotProcessor)
    }

    private val newUser = TestParameters(
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
        useBrowser = null,
        authorizationAction = AuthorizeAction.Authorize,
        takeScreenshotOnLpmLoad = true
    )

    @Test
    fun testCard() {
        testDriver.confirmNewOrGuestComplete(
            newUser.copy(
                billing = Billing.On,
                paymentMethod = LpmRepository.HardcodedCard,
                authorizationAction = null,
                saveForFutureUseCheckboxVisible = true,
                saveCheckboxValue = false,
            )
        )
    }

    @Test
    fun testBancontact() {
        testDriver.confirmNewOrGuestComplete(
            newUser.copy(
                paymentMethod = lpmRepository.fromCode("bancontact")!!,
                authorizationAction = AuthorizeAction.Authorize,
            )
        )
    }

    @Test
    fun testSepaDebit() {
        testDriver.confirmNewOrGuestComplete(
            newUser.copy(
                paymentMethod = lpmRepository.fromCode("sepa_debit")!!,
                authorizationAction = null,
                automatic = Automatic.Off,
                delayed = DelayedPMs.On
            )
        ) {
            composeTestRule.onNodeWithText("IBAN").apply {
                performTextInput(
                    "DE89370400440532013000"
                )
            }
        }
    }

    @Test
    fun testIdeal() {
        testDriver.confirmNewOrGuestComplete(
            newUser.copy(
                paymentMethod = lpmRepository.fromCode("ideal")!!,
                authorizationAction = AuthorizeAction.Authorize,
            )
        )
    }

    @Test
    fun testEps() {
        testDriver.confirmNewOrGuestComplete(
            newUser.copy(
                paymentMethod = lpmRepository.fromCode("eps")!!,
                authorizationAction = AuthorizeAction.Authorize,
            )
        )
    }

    @Test
    fun testGiropay() {
        testDriver.confirmNewOrGuestComplete(
            newUser.copy(
                paymentMethod = lpmRepository.fromCode("giropay")!!,
                authorizationAction = AuthorizeAction.Authorize,
            )
        )
    }

    @Test
    fun testP24() {
        testDriver.confirmNewOrGuestComplete(
            newUser.copy(
                paymentMethod = lpmRepository.fromCode("p24")!!,
                authorizationAction = AuthorizeAction.Authorize,
            )
        )
    }

    @Test
    fun testAfterpay() {
        testDriver.confirmNewOrGuestComplete(
            newUser.copy(
                paymentMethod = lpmRepository.fromCode("afterpay_clearpay")!!,
                authorizationAction = AuthorizeAction.Authorize,
                currency = Currency.USD,
                shipping = Shipping.On
            )
        )
    }

    @Test
    fun testSofort() {
        testDriver.confirmNewOrGuestComplete(
            newUser.copy(
                paymentMethod = lpmRepository.fromCode("sofort")!!,
                authorizationAction = AuthorizeAction.Authorize,
                currency = Currency.EUR,
                delayed = DelayedPMs.On,
                automatic = Automatic.Off
            )
        )
    }

    @Test
    fun testAffirm() {
        testDriver.confirmNewOrGuestComplete(
            newUser.copy(
                paymentMethod = lpmRepository.fromCode("affirm")!!,
                authorizationAction = AuthorizeAction.Authorize,
                currency = Currency.USD,
                shipping = Shipping.On,
                automatic = Automatic.Off
            )
        )
    }

    @Test
    fun testAuBecsDD() {
        testDriver.confirmNewOrGuestComplete(
            newUser.copy(
                paymentMethod = lpmRepository.fromCode("au_becs_debit")!!,
                authorizationAction = null,
                currency = Currency.AUD,
                shipping = Shipping.On,
                delayed = DelayedPMs.On,
                automatic = Automatic.Off,
                merchantCountryCode = "AU"
            )
        )
    }

    @Ignore("Complex authorization handling required")
    fun testKlarna() {
        testDriver.confirmNewOrGuestComplete(
            newUser.copy(
                paymentMethod = lpmRepository.fromCode("klarna")!!,
                authorizationAction = AuthorizeAction.Authorize,
                currency = Currency.USD
            )
        )
    }

    @Test
    fun testPayPal() {
        testDriver.confirmNewOrGuestComplete(
            newUser.copy(
                paymentMethod = lpmRepository.fromCode("paypal")!!,
                authorizationAction = AuthorizeAction.Authorize,
                currency = Currency.GBP,
                automatic = Automatic.Off
            )
        )
    }
}
