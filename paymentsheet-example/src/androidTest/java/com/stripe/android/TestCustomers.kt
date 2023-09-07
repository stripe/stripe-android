package com.stripe.android

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
import com.stripe.android.test.core.PlaygroundTestDriver
import com.stripe.android.test.core.Shipping
import com.stripe.android.test.core.TestParameters
import com.stripe.android.utils.TestRules
import com.stripe.android.utils.initializedLpmRepository
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestCustomers {

    @get:Rule
    val rules = TestRules.create()

    private lateinit var device: UiDevice
    private lateinit var testDriver: PlaygroundTestDriver

    @Before
    fun before() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        testDriver = PlaygroundTestDriver(device, rules.compose)
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
        authorizationAction = AuthorizeAction.AuthorizePayment,
        merchantCountryCode = "GB",
    )

    @Test
    fun testAuthorizeGuest() {
        testDriver.confirmNewOrGuestComplete(
            bancontactNewUser.copy(
                customer = Customer.Guest,
            )
        )
    }

    @Test
    fun testAuthorizeNew() {
        testDriver.confirmNewOrGuestComplete(
            bancontactNewUser.copy(
                customer = Customer.New,
            )
        )
    }

    companion object {
        private val lpmRepository = initializedLpmRepository(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
        )
    }
}
