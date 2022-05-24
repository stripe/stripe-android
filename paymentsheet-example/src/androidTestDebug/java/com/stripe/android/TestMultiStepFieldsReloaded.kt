package com.stripe.android

import androidx.compose.ui.test.assertContentDescriptionEquals
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
import com.stripe.android.ui.core.elements.LpmRepository
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class TestMultiStepFieldsReloaded {
    @get:Rule
    var globalTimeout: Timeout = Timeout.seconds(INDIVIDUAL_TEST_TIMEOUT_SECONDS)

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @get:Rule
    val testWatcher = TestWatcher()

    private lateinit var device: UiDevice
    private lateinit var testDriver: PlaygroundTestDriver

    private val newUser = TestParameters(
        paymentMethod = lpmRepository.fromCode("bancontact")!!,
        Customer.New,
        GooglePayState.Off,
        Currency.EUR,
        IntentType.Pay,
        Billing.Off,
        shipping = Shipping.Off,
        delayed = DelayedPMs.Off,
        automatic = Automatic.On,
        saveCheckboxValue = false,
        saveForFutureUseCheckboxVisible = false,
        useBrowser = null,
        authorizationAction = AuthorizeAction.Authorize,
    )

    @Before
    fun before() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        testDriver = PlaygroundTestDriver(device, composeTestRule, screenshotProcessor)
    }

    @Test
    fun testCard() {
        testDriver.confirmCustom(
            newUser.copy(
                paymentMethod = lpmRepository.getCard(),
                saveCheckboxValue = true,
                saveForFutureUseCheckboxVisible = true,
            )
        )
    }

    @Test
    fun testBancontact() {
        testDriver.confirmCustom(
            newUser.copy(
                paymentMethod = lpmRepository.fromCode("bancontact")!!,
            )
        )
    }

    @Test
    fun testSepaDebit() {
        testDriver.confirmCustom(
            newUser.copy(
                paymentMethod = lpmRepository.fromCode("sepa_debit")!!,
                delayed = DelayedPMs.On,
                authorizationAction = null
            ),
            populateCustomLpmFields = {
                composeTestRule.onNodeWithText("IBAN").apply {
                    performTextInput(
                        "DE89370400440532013000"
                    )
                }
            },
            verifyCustomLpmFields = {
                composeTestRule.onNodeWithText("IBAN").apply {
                    assertContentDescriptionEquals(
                        "DE89370400440532013000"
                    )
                }
            }

        )
    }

    @Test
    fun testIdeal() {
        testDriver.confirmCustom(
            newUser.copy(
                paymentMethod = lpmRepository.fromCode("ideal")!!,
            )
        )
    }

    @Test
    fun testEps() {
        testDriver.confirmCustom(
            newUser.copy(
                paymentMethod = lpmRepository.fromCode("eps")!!,
            )
        )
    }

    @Test
    fun testGiropay() {
        testDriver.confirmCustom(
            newUser.copy(
                paymentMethod = lpmRepository.fromCode("giropay")!!,
            )
        )
    }

    @Test
    fun testP24() {
        testDriver.confirmCustom(
            newUser.copy(
                paymentMethod = lpmRepository.fromCode("p24")!!,
            )
        )
    }

    @Test
    fun testAfterpay() {
        testDriver.confirmCustom(
            newUser.copy(
                paymentMethod = lpmRepository.fromCode("afterpay_clearpay")!!,
                currency = Currency.USD,
                shipping = Shipping.On
            )
        )
    }

    @Test
    fun testAffirm() {
        testDriver.confirmCustom(
            newUser.copy(
                paymentMethod = lpmRepository.fromCode("affirm")!!,
                currency = Currency.USD,
            )
        )
    }

    @Test
    fun testAuBecsDD() {
        testDriver.confirmCustom(
            newUser.copy(
                paymentMethod = lpmRepository.fromCode("au_becs_debit")!!,
                delayed = DelayedPMs.On,
                currency = Currency.AUD,
            )
        )
    }

    @Ignore("Complex authorization handling required")
    fun testKlarna() {
        testDriver.confirmCustom(
            newUser.copy(
                paymentMethod = lpmRepository.fromCode("klarna")!!,
            )
        )
    }

    @Ignore("Need to add GBP currency to playground")
    fun testPayPal() {
        testDriver.confirmCustom(
            newUser.copy(
                paymentMethod = lpmRepository.fromCode("paypal")!!,
            )
        )
    }

    companion object {
        // There exists only one screenshot processor so that all tests put
        // their files in the same directory.
        private val screenshotProcessor = MyScreenCaptureProcessor()
        private val lpmRepository = LpmRepository(
            InstrumentationRegistry.getInstrumentation().targetContext.resources
        )
    }
}
