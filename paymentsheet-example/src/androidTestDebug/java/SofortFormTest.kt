import android.util.Log
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.PerformException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.activity.PaymentSheetPlaygroundActivity
import com.stripe.android.paymentsheet.example.activity.PaymentSheetPlaygroundActivity.Companion.multiStepUIIdlingResource
import com.stripe.android.paymentsheet.example.activity.PaymentSheetPlaygroundActivity.Companion.singleStepUIIdlingResource
import com.stripe.android.paymentsheet.matchFirstPaymentOptionsHolder
import com.stripe.android.paymentsheet.matchPaymentMethodHolder
import com.stripe.android.paymentsheet.transitionFragmentResource
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
internal class SofortFormTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private lateinit var device: UiDevice

    @Before
    fun before() {
        androidx.test.espresso.intent.Intents.init()
        device = UiDevice.getInstance(getInstrumentation())
    }

    // TODO: Need to revist the idle resource construction - should probably be dependency injected

    @Test
    fun returningCustomerComplete() {
        runComplete(
            TestParameters(
                Customer.Returning,
                GooglePayState.Off,
                Currency.EUR,
                Checkout.Pay,
                Billing.Off
            ),
        ) {
            try {
                onView(withId(com.stripe.android.paymentsheet.R.id.recycler))
                    .check(matches(isDisplayed()))
                    .perform(
                        RecyclerViewActions.actionOnHolderItem(
                            matchFirstPaymentOptionsHolder("4242"),
                            click()
                        )
                    )
            } catch (e: PerformException) {
                // Item is already selected.
            }
        }
    }

    @Test
    fun newUserComplete() {
        val testParameters = TestParameters(
            Customer.New,
            GooglePayState.Off,
            Currency.EUR,
            Checkout.Pay,
            Billing.Off
        )
        runComplete(
            testParameters,
        ) {
            try {
                onView(withId(com.stripe.android.paymentsheet.R.id.payment_methods_recycler))
                    .check(matches(isDisplayed()))
                    .perform(
                        RecyclerViewActions.actionOnHolderItem(
                            matchPaymentMethodHolder("iDEAL"),
                            click()
                        )
                    )
            } catch (e: PerformException) {
                // Item is already selected.
            }

            // TODO: Need to fill in these fields based on form spec.
            composeTestRule.onNodeWithText("Name").performTextInput("My name")
            composeTestRule.onNodeWithText("Email").performTextInput("email@email.com")

            // TODO: Would really like ot see verification of the checkbox presence and
            // the mandate existence.

            closeSoftKeyboard()
        }
    }

    private fun runComplete(testParameters: TestParameters, logic: () -> Unit) {
        val scenario = launch(PaymentSheetPlaygroundActivity::class.java)

        // TODO: I can imagine a different test that checks the different authorization states.
        val authorization = AuthorizationParameters(
            Browser.Chrome,
            AuthorizeAction.Authorize
        )
        var resultValue: String? = null
        val callbackLock = Semaphore(1)
        scenario.onActivity { activity ->
            callbackLock.acquire()
            (activity as PaymentSheetPlaygroundActivity).viewModel.status.observeForever {
                Log.e("STRIPE", "GOT THE STRING!!! $it ***")
                resultValue = it
                callbackLock.release()
            }
        }

        testParameters.customer.click()
        testParameters.googlePayState.click()
        testParameters.currency.click()
        testParameters.checkout.click()
        testParameters.billing.click()

        IdlingRegistry.getInstance().register(singleStepUIIdlingResource)
        IdlingRegistry.getInstance().register(multiStepUIIdlingResource)
        IdlingRegistry.getInstance().register(transitionFragmentResource)

        EspressoLabelIdButton(R.string.reload_paymentsheet).click()
        onView(withText(R.string.checkout_complete)).perform(click())

        logic()


        onView(withId(R.id.buy_button)).perform(click())

        // TODO: Need to rid of this sleep
        TimeUnit.SECONDS.sleep(1)
        if (SelectBrowserWindow.exists(device)) {
            authorization.browser.click(device)
        }

        if (AuthorizeWindow.exists(device, authorization.browser)) {
            AuthorizePageLoaded.blockUntilLoaded(device)
            when (authorization.action) {
                AuthorizeAction.Authorize, AuthorizeAction.Fail ->
                    authorization.action.click(device)
                AuthorizeAction.Cancel ->
                    device.pressBack()
            }
        }

        // TODO: Need to handle cancel/fail authorization - do it again but press success this time?

        IdlingRegistry.getInstance().unregister(singleStepUIIdlingResource)
        IdlingRegistry.getInstance().unregister(multiStepUIIdlingResource)
        IdlingRegistry.getInstance().unregister(transitionFragmentResource)

        callbackLock.acquire()
        assertThat(resultValue).isEqualTo("Completed")
        callbackLock.release()
    }


    data class TestParameters(
        val customer: Customer,
        val googlePayState: GooglePayState,
        val currency: Currency,
        val checkout: Checkout,
        val billing: Billing,
    )

    data class AuthorizationParameters(
        val browser: Browser,
        val action: AuthorizeAction
    )
}
