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
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestPlatformInfrastructureTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private lateinit var device: UiDevice
    private lateinit var testDriver: PlaygroundTestDriver
    private val screenshotProcessor = MyScreenCaptureProcessor()

    @Before
    fun before() {
        androidx.test.espresso.intent.Intents.init()
        device = UiDevice.getInstance(getInstrumentation())
        testDriver = PlaygroundTestDriver(device, composeTestRule, screenshotProcessor)
    }

    @After
    fun after() {
        androidx.test.espresso.intent.Intents.release()
    }

    private val bancontactNewUser = TestParameters(
        SupportedPaymentMethod.Bancontact,
        Customer.New,
        GooglePayState.On,
        Currency.EUR,
        Checkout.Pay,
        Billing.Off,
        automatic = Automatic.On,
        delayed = DelayedPMs.Off,
        saveCheckboxValue = false,
        useBrowser = Browser.Chrome,
        authorizationAction = AuthorizeAction.Authorize,
        saveForFutureUseCheckboxVisible = false
    )

    @Test
    fun testAuthorizeChrome() = runBlocking {
        testDriver.confirmNewOrGuestCompleteSuccess(
            bancontactNewUser.copy(
                useBrowser = Browser.Chrome,
            )
        )
    }

    @Ignore("Not able to click on authorize correctly")
    fun testAuthorizeFirefox() = runBlocking {
        testDriver.confirmNewOrGuestCompleteSuccess(
            bancontactNewUser.copy(
                useBrowser = Browser.Firefox,
            )
        )
    }

    @Test
    fun testGooglePayOn() = runBlocking {
        testDriver.confirmNewOrGuestCompleteSuccess(
            bancontactNewUser.copy(
                googlePayState = GooglePayState.On,
            )
        )
    }

    @Test
    fun scrollToLpmAndCustomFields() = runBlocking {
        testDriver.confirmNewOrGuestCompleteSuccess(
            bancontactNewUser.copy(
                paymentMethod = SupportedPaymentMethod.SepaDebit,
                automatic = Automatic.Off,
                delayed = DelayedPMs.On,
                saveForFutureUseCheckboxVisible = false,
                saveCheckboxValue = false
            )
        ) {
            composeTestRule.onNodeWithText("IBAN")
                .performTextInput("DE89370400440532013000")
        }
    }

    // TODO verify other browsers are working
    // TODO: Need to revisit the idle resource construction - should probably be dependency injected
    // TODO: I can imagine a different test that checks the different authorization states.
    // TODO: Need a card test when compose is in.
    // Dropdown
    // Other address fields

    // Scroll to any item in the list
    // Address field

}
