package com.stripe.android

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
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
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestFieldPopulation {
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

    private val sepaDebit = TestParameters(
        paymentMethod = lpmRepository.fromCode("sepa_debit")!!,
        customer = Customer.New,
        googlePayState = GooglePayState.On,
        currency = Currency.EUR,
        intentType = IntentType.Pay,
        billing = Billing.On,
        shipping = Shipping.Off,
        delayed = DelayedPMs.On,
        automatic = Automatic.Off,
        saveCheckboxValue = false,
        saveForFutureUseCheckboxVisible = false,
        useBrowser = null,
        authorizationAction = null,
    )

    private val card = TestParameters(
        paymentMethod = LpmRepository.HardcodedCard,
        customer = Customer.New,
        googlePayState = GooglePayState.On,
        currency = Currency.EUR,
        intentType = IntentType.Pay,
        billing = Billing.On,
        shipping = Shipping.Off,
        delayed = DelayedPMs.On,
        automatic = Automatic.Off,
        saveCheckboxValue = false,
        saveForFutureUseCheckboxVisible = true,
        useBrowser = null,
        authorizationAction = null
    )

    @Ignore("Testing of dropdowns is not yet supported")
    fun testDropdowns() {

    }

    @Test
    fun testCardDefaultAddress() {
        testDriver.confirmNewOrGuestComplete(
            card.copy(
                billing = Billing.On,
                authorizationAction = null,
                saveForFutureUseCheckboxVisible = true,
                saveCheckboxValue = true,
            )
        )
    }

    @Test
    fun testCardNoDefaultAddress() {
        testDriver.confirmNewOrGuestComplete(
            card.copy(
                billing = Billing.Off,
                authorizationAction = null,
                saveForFutureUseCheckboxVisible = true,
                saveCheckboxValue = false,
            )
        )
    }

    @Test
    fun testPopulateCustomFields() {
        // name, email, iban, line1, city, county, zip
        testDriver.confirmNewOrGuestComplete(
            sepaDebit.copy(billing = Billing.Off)
        ) {
            composeTestRule.onNodeWithText("IBAN")
                .performTextInput("DE89370400440532013000")
        }
    }

    @Test
    fun testDontPopulateWhenDefaultBillingAddress() {
        testDriver.confirmNewOrGuestComplete(
            sepaDebit.copy(billing = Billing.On)
        ) {
            composeTestRule.onNodeWithText("IBAN")
                .performTextInput("DE89370400440532013000")
        }
    }

    companion object {
        private val lpmRepository = LpmRepository(
            InstrumentationRegistry.getInstrumentation().targetContext.resources
        )
    }
}
