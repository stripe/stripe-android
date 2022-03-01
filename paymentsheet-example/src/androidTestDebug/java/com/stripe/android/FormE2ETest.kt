package com.stripe.android

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class FormE2ETest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private lateinit var device: UiDevice
    private val screenshotProcessor = MyScreenCaptureProcessor()

    @Before
    fun before() {
        androidx.test.espresso.intent.Intents.init()
        device = UiDevice.getInstance(getInstrumentation())
    }

    @After
    fun after() {
        androidx.test.espresso.intent.Intents.release()
    }

    // TODO: Need to revist the idle resource construction - should probably be dependency injected

    @Test
    fun testBancontact() = runBlocking {
        val testParameters = TestParameters(
            SupportedPaymentMethod.Bancontact,
            Customer.New,
            GooglePayState.Off,
            Currency.EUR,
            Checkout.Pay,
            Billing.On,
            automatic = Automatic.Off,
            delayed = DelayedPMs.Off,
            saveCheckboxValue = false,
            authorizationParameters = AuthorizationParameters(
                Browser.Chrome,
                AuthorizeAction.Authorize
            ),
            saveForFutureUseCheckboxVisible = false
        )
        confirmCompleteSuccess(device, composeTestRule, screenshotProcessor, testParameters)
    }

    @Test
    fun testSepaDebit() = runBlocking {
        val testParameters = TestParameters(
            SupportedPaymentMethod.SepaDebit,
            Customer.New,
            GooglePayState.Off,
            Currency.EUR,
            Checkout.Pay,
            Billing.Off,
            automatic = Automatic.Off,
            delayed = DelayedPMs.On,
            saveForFutureUseCheckboxVisible = false,
            saveCheckboxValue = false,
            authorizationParameters = null,
        )
        confirmCompleteSuccess(device, composeTestRule, screenshotProcessor, testParameters) {
            composeTestRule.onNodeWithText("IBAN")
                .performTextInput("DE89370400440532013000")

        }
    }

    // TODO verify other browsers are working
}
