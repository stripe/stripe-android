package com.stripe.android

import androidx.test.espresso.intent.Intents
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
import com.stripe.android.test.core.IntentType
import com.stripe.android.test.core.LinkState
import com.stripe.android.test.core.MyScreenCaptureProcessor
import com.stripe.android.test.core.PlaygroundTestDriver
import com.stripe.android.test.core.Shipping
import com.stripe.android.test.core.TestParameters
import com.stripe.android.utils.TestRules
import com.stripe.android.utils.initializedLpmRepository
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestAuthorization {

    @get:Rule
    val rules = TestRules.create()

    private lateinit var device: UiDevice
    private lateinit var testDriver: PlaygroundTestDriver
    private val screenshotProcessor = MyScreenCaptureProcessor()

    @Before
    fun before() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        testDriver = PlaygroundTestDriver(device, rules.compose, screenshotProcessor)
    }

    @After
    fun after() {
        Intents.release()
    }

    private val bancontactNewUser = TestParameters(
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
        useBrowser = Browser.Chrome,
        authorizationAction = AuthorizeAction.Authorize,
        merchantCountryCode = "GB",
    )

    @Test
    fun testAuthorizeSuccess() {
        testDriver.confirmNewOrGuestComplete(
            bancontactNewUser.copy(
                authorizationAction = AuthorizeAction.Authorize,
            )
        )
    }

    @Test
    fun testAuthorizeFailure() {
        testDriver.confirmNewOrGuestComplete(
            bancontactNewUser.copy(
                paymentMethod = lpmRepository.fromCode("ideal")!!,
                authorizationAction = AuthorizeAction.Fail(
                    expectedError = "We are unable to authenticate your payment method. " +
                        "Please choose a different payment method and try again.",
                ),
            )
        )
    }

    @Test
    fun testAuthorizeCancel() {
        testDriver.confirmNewOrGuestComplete(
            bancontactNewUser.copy(
                authorizationAction = AuthorizeAction.Cancel,
            )
        )
    }

    companion object {
        private val lpmRepository = initializedLpmRepository(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
        )
    }
}
