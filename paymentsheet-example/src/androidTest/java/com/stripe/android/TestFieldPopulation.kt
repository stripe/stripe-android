package com.stripe.android

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.stripe.android.model.PaymentMethod
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.Automatic
import com.stripe.android.test.core.Billing
import com.stripe.android.test.core.Browser
import com.stripe.android.test.core.Currency
import com.stripe.android.test.core.Customer
import com.stripe.android.test.core.DelayedPMs
import com.stripe.android.test.core.GooglePayState
import com.stripe.android.test.core.IntentType
import com.stripe.android.test.core.LinkState
import com.stripe.android.test.core.PlaygroundTestDriver
import com.stripe.android.test.core.Shipping
import com.stripe.android.test.core.TestParameters
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.utils.TestRules
import com.stripe.android.utils.initializedLpmRepository
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestFieldPopulation {

    @get:Rule
    val rules = TestRules.create()

    private lateinit var device: UiDevice
    private lateinit var testDriver: PlaygroundTestDriver

    @Before
    fun before() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        testDriver = PlaygroundTestDriver(device, rules.compose)
    }

    private val sepaDebit = TestParameters(
        paymentMethod = lpmRepository.fromCode("sepa_debit")!!,
        customer = Customer.New,
        linkState = LinkState.Off,
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
        merchantCountryCode = "GB",
    )

    private val card = TestParameters(
        paymentMethod = LpmRepository.HardcodedCard,
        customer = Customer.New,
        linkState = LinkState.Off,
        googlePayState = GooglePayState.On,
        currency = Currency.EUR,
        merchantCountryCode = "GB",
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

    private val bancontact = TestParameters(
        paymentMethod = lpmRepository.fromCode("bancontact")!!,
        customer = Customer.New,
        linkState = LinkState.Off,
        googlePayState = GooglePayState.Off,
        currency = Currency.EUR,
        intentType = IntentType.Pay,
        billing = Billing.Off,
        shipping = Shipping.Off,
        delayed = DelayedPMs.Off,
        automatic = Automatic.Off,
        saveCheckboxValue = false,
        saveForFutureUseCheckboxVisible = false,
        useBrowser = Browser.Chrome,
        authorizationAction = AuthorizeAction.AuthorizePayment,
        merchantCountryCode = "GB",
        supportedPaymentMethods = listOf(
            PaymentMethod.Type.Card.code,
            PaymentMethod.Type.Bancontact.code,
        ),
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
            rules.compose.onNodeWithText("IBAN")
                .performTextInput("DE89370400440532013000")
        }
    }

    @Test
    fun testDontPopulateWhenDefaultBillingAddress() {
        testDriver.confirmNewOrGuestComplete(
            sepaDebit.copy(billing = Billing.On)
        ) {
            rules.compose.onNodeWithText("IBAN")
                .performTextInput("DE89370400440532013000")
        }
    }

    @Test
    fun testSinglePaymentMethodWithoutGooglePayAndKeyboardInput() {
        testDriver.confirmNewOrGuestComplete(bancontact) {
            rules.compose.waitForIdle()
            val node = rules.compose.onNodeWithText("Full name")
            node.performClick()
            rules.compose.waitForIdle()
            node.performTextInput("Jenny Rosen")
            rules.compose.waitForIdle()
        }
    }

    companion object {
        private val lpmRepository = initializedLpmRepository(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
        )
    }
}
