import android.util.Log
import androidx.annotation.IntegerRes
import androidx.annotation.StringRes
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.PerformException
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.activity.PaymentSheetPlaygroundActivity
import com.stripe.android.paymentsheet.example.activity.PaymentSheetPlaygroundActivity.Companion.multiStepUIIdlingResource
import com.stripe.android.paymentsheet.example.activity.PaymentSheetPlaygroundActivity.Companion.singleStepUIIdlingResource
import com.stripe.android.paymentsheet.matchFirstPaymentOptionsHolder
import com.stripe.android.paymentsheet.matchPaymentMethodHolder
import com.stripe.android.paymentsheet.transitionFragmentResource
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.security.InvalidParameterException
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

    @After
    fun after() {
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
            Billing.Off,
            AuthorizationParameters(
                Browser.Opera,
                AuthorizeAction.Cancel
            )
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

            closeSoftKeyboard()
        }
    }

    private fun runComplete(testParameters: TestParameters, logic: () -> Unit) {
        val scenario = launch(PaymentSheetPlaygroundActivity::class.java)
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
            testParameters.authorization.browser.click(device)
        }

        if (AuthorizeWindow.exists(device, testParameters.authorization.browser)) {
            AuthorizePageLoaded.blockUntilLoaded(device)
            when (testParameters.authorization.action) {
                AuthorizeAction.Authorize, AuthorizeAction.Fail ->
                    testParameters.authorization.action.click(device)
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

    open class EspressoLabelIdButton(@StringRes val label: Int) {
        fun click() {
            onView(withText(label)).perform(ViewActions.click())
        }
    }

    abstract class EspressoIdButton(@IntegerRes val id: Int) {
        fun click() {
            onView(withId(id)).perform(ViewActions.click())
        }
    }

    open class UiAutomatorText(
        private val label: String,
        className: String = "android.widget.TextView"
    ) {
        private val selector = UiSelector().textContains(label)
            .className(className)

        fun click(device: UiDevice) {
            if (!exists(device)) {
                throw InvalidParameterException("Text button not found: $label")
            }
            device.findObject(selector).click()
        }

        fun exists(device: UiDevice) =
            device.findObject(selector).exists()
    }

    sealed class PaymentSelection(@StringRes id: Int) : EspressoLabelIdButton(id) {
        object Card : PaymentSelection(R.string.stripe_paymentsheet_payment_method_card)
        object Sofort : PaymentSelection(R.string.stripe_paymentsheet_payment_method_sofort)
        object iDEAL : PaymentSelection(R.string.stripe_paymentsheet_payment_method_ideal)
        object Bancontact : PaymentSelection(R.string.stripe_paymentsheet_payment_method_bancontact)
        object SepaDebit : PaymentSelection(R.string.stripe_paymentsheet_payment_method_sepa_debit)
        object Eps : PaymentSelection(R.string.stripe_paymentsheet_payment_method_eps)
    }

    data class TestParameters(
        val customer: Customer,
        val googlePayState: GooglePayState,
        val currency: Currency,
        val checkout: Checkout,
        val billing: Billing,
        val authorization: AuthorizationParameters = AuthorizationParameters(
            Browser.Chrome,
            AuthorizeAction.Authorize
        )
    )

    data class AuthorizationParameters(
        val browser: Browser,
        val action: AuthorizeAction
    )

    sealed class Billing(@IntegerRes id: Int) : EspressoIdButton(id) {
        object On : Billing(R.id.default_billing_on_button)
        object Off : Billing(R.id.default_billing_off_button)
    }

    sealed class Checkout(@StringRes id: Int) : EspressoLabelIdButton(id) {
        object Pay : Checkout(R.string.payment)
        object PayWithSetup : Checkout(R.string.payment_with_setup)
        object Setup : Checkout(R.string.setup)
    }

    sealed class Currency(@StringRes id: Int) : EspressoLabelIdButton(id) {
        object USD : Currency(R.string.currency_usd)
        object EUR : Currency(R.string.currency_eur)
    }

    sealed class GooglePayState(@IntegerRes id: Int) : EspressoIdButton(id) {
        object On : GooglePayState(R.id.google_pay_on_button)
        object Off : GooglePayState(R.id.google_pay_off_button)
    }

    sealed class Customer(@StringRes id: Int) : EspressoLabelIdButton(id) {
        object Guest : Customer(R.string.customer_guest)
        object New : Customer(R.string.customer_new)
        object Returning : Customer(R.string.customer_returning)
    }

    // No 3DS2 support
    sealed class AuthorizeAction(
        name: String,
        className: String
    ) : UiAutomatorText(name, className) {
        object Authorize : AuthorizeAction("AUTHORIZE TEST PAYMENT", "android.widget.Button")
        object Cancel : AuthorizeAction("", "")
        object Fail : AuthorizeAction("FAIL TEST PAYMENT", "android.widget.Button")
    }

    object SelectBrowserWindow : UiAutomatorText("Verify your payment")
    object AuthorizeWindow {
        fun exists(device: UiDevice, browser: Browser?) =
            device.findObject(getSelector(browser)).exists()

        private fun getSelector(browser: Browser?) = UiSelector()
            .packageName(browser?.packageName)
            .resourceId(browser?.resourdID)
    }

    object AuthorizePageLoaded {
        fun blockUntilLoaded(device: UiDevice) {
            device.wait(
                Until.findObject(
                    By.textContains("test payment page")
                ),
                10000
            )
        }
    }

    sealed class Browser(name: String, val packageName: String) :
        UiAutomatorText(name) {
        val resourdID = "$packageName:id/action_bar_root"

        object Chrome : Browser("Chrome", "com.android.chrome")
        object Opera : Browser("Opera", "com.opera.browser")
        object Firefox : Browser("Firefox", "org.mozilla.firefox")
    }
}
