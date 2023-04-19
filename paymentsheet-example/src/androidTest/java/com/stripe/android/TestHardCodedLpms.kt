package com.stripe.android

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.Automatic
import com.stripe.android.test.core.Billing
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
class TestHardCodedLpms {

    @get:Rule
    val rules = TestRules.create()

    private lateinit var device: UiDevice
    private lateinit var testDriver: PlaygroundTestDriver

    companion object {
        private val lpmRepository = initializedLpmRepository(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
        )
    }

    @Before
    fun before() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        testDriver = PlaygroundTestDriver(device, rules.compose)
    }

    private val newUser = TestParameters(
        paymentMethod = lpmRepository.fromCode("bancontact")!!,
        customer = Customer.New,
        linkState = LinkState.Off,
        googlePayState = GooglePayState.On,
        currency = Currency.EUR,
        intentType = IntentType.Pay,
        billing = Billing.On,
        shipping = Shipping.Off,
        delayed = DelayedPMs.Off,
        automatic = Automatic.Off,
        saveCheckboxValue = false,
        saveForFutureUseCheckboxVisible = false,
        useBrowser = null,
        authorizationAction = AuthorizeAction.Authorize,
        merchantCountryCode = "GB"
    )

    @Test
    fun testCard() {
        testDriver.confirmNewOrGuestComplete(
            newUser.copy(
                customer = Customer.New,
                billing = Billing.On,
                paymentMethod = LpmRepository.HardcodedCard,
                authorizationAction = null,
                saveForFutureUseCheckboxVisible = true,
                saveCheckboxValue = false,
            )
        )
    }

    @Test
    fun testCardWithCustomBillingDetailsCollection() {
        testDriver.confirmNewOrGuestComplete(
            newUser.copy(
                customer = Customer.New,
                billing = Billing.On,
                paymentMethod = LpmRepository.HardcodedCard,
                authorizationAction = null,
                saveForFutureUseCheckboxVisible = true,
                saveCheckboxValue = false,
                attachDefaults = false,
                collectName = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                collectEmail = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                collectPhone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
                collectAddress = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            ),
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
            rules.compose.onNodeWithText("IBAN").apply {
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
                merchantCountryCode = "US",
                currency = Currency.USD,
                shipping = Shipping.OnWithDefaults
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
                merchantCountryCode = "GB",
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
                merchantCountryCode = "US",
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
                currency = Currency.USD,
                merchantCountryCode = "US",
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
                merchantCountryCode = "GB",
                automatic = Automatic.Off
            )
        )
    }

    @Test
    fun testUpi() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = newUser.copy(
                paymentMethod = lpmRepository.fromCode("upi")!!,
                currency = Currency.INR,
                merchantCountryCode = "IN",
                automatic = Automatic.Off,
                authorizationAction = null,
            ),
            populateCustomLpmFields = {
                rules.compose.onNodeWithText("UPI ID").apply {
                    performTextInput(
                        "payment.success@stripeupi"
                    )
                }
            }
        )
    }

    @Test
    fun testCashAppPay_Success() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = newUser.copy(
                paymentMethod = lpmRepository.fromCode("cashapp")!!,
                currency = Currency.USD,
                merchantCountryCode = "US",
                authorizationAction = AuthorizeAction.Authorize,
                supportedPaymentMethods = listOf("card", "cashapp"),
            ),
        )
    }

    @Test
    fun testCashAppPay_Fail() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = newUser.copy(
                paymentMethod = lpmRepository.fromCode("cashapp")!!,
                currency = Currency.USD,
                merchantCountryCode = "US",
                authorizationAction = AuthorizeAction.Fail(
                    expectedError = "The customer declined this payment.",
                ),
                supportedPaymentMethods = listOf("card", "cashapp"),
            ),
        )
    }

    @Test
    fun testCashAppPay_Cancel() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = newUser.copy(
                paymentMethod = lpmRepository.fromCode("cashapp")!!,
                currency = Currency.USD,
                merchantCountryCode = "US",
                authorizationAction = AuthorizeAction.Cancel,
                supportedPaymentMethods = listOf("card", "cashapp"),
            ),
        )
    }
}
