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
import java.util.concurrent.Semaphore

@RunWith(AndroidJUnit4::class)
internal class SofortFormTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun before() {
        androidx.test.espresso.intent.Intents.init()
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
            completed = true,
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
        runComplete(
            TestParameters(
                Customer.New,
                GooglePayState.Off,
                Currency.EUR,
                Checkout.Pay,
                Billing.Off
            ),
            completed = true,
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

            composeTestRule.onNodeWithText("Name").performTextInput("My name")
            composeTestRule.onNodeWithText("Email").performTextInput("email@email.com")

            closeSoftKeyboard()
        }
    }

    private fun runComplete(testParameters: TestParameters, completed: Boolean, logic: () -> Unit) {
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

        LabelIdButton(R.string.reload_paymentsheet).click()
        onView(withText(R.string.checkout_complete)).perform(click())

        logic()

        IdlingRegistry.getInstance().unregister(singleStepUIIdlingResource)
        IdlingRegistry.getInstance().unregister(multiStepUIIdlingResource)
        IdlingRegistry.getInstance().unregister(transitionFragmentResource)

        onView(withId(R.id.buy_button)).perform(click())

        if (completed) {
            callbackLock.acquire()
            assertThat(resultValue).isEqualTo("Completed")
            callbackLock.release()
        }
    }

    /**
    //     * Perform action of waiting until the element is accessible & not shown.
    //     * @param viewId The id of the view to wait for.
    //     * @param millis The timeout of until when to wait for.
    //     */
//    fun waitUntilShown(viewId: Int, millis: Long): ViewAction? {
//        return object : ViewAction {
//            override fun getConstraints(): Matcher<View> {
//                return isRoot()
//            }
//
//            override fun getDescription(): String {
//                return "wait for a specific view with id <$viewId> is shown during $millis millis."
//            }
//
//            override fun perform(uiController: UiController, view: View?) {
//                uiController.loopMainThreadUntilIdle()
//                val startTime = System.currentTimeMillis()
//                val endTime = startTime + millis
//                val viewMatcher: Matcher<View> = withId(viewId)
//                do {
//                    for (child in TreeIterables.breadthFirstViewTraversal(view)) {
//                        // found view with required ID
//                        if (viewMatcher.matches(child) && child.isShown) {
//                            return
//                        }
//                    }
//                    uiController.loopMainThreadForAtLeast(50)
//                } while (System.currentTimeMillis() < endTime)
//                throw PerformException.Builder()
//                    .withActionDescription(this.description)
//                    .withViewDescription(HumanReadables.describe(view))
//                    .withCause(TimeoutException())
//                    .build()
//            }
//        }
//    }
    open class LabelIdButton(@StringRes val label: Int) {
        fun click() {
            onView(withText(label)).perform(ViewActions.click())
        }
    }

    abstract class IdButton(@IntegerRes val id: Int) {
        fun click() {
            onView(withId(id)).perform(ViewActions.click())
        }
    }

    sealed class PaymentSelection(@StringRes id: Int) : LabelIdButton(id) {
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
        val billing: Billing
    )

    sealed class Billing(@IntegerRes id: Int) : IdButton(id) {
        object On : Billing(R.id.default_billing_on_button)
        object Off : Billing(R.id.default_billing_off_button)
    }

    sealed class Checkout(@StringRes id: Int) : LabelIdButton(id) {
        object Pay : Checkout(R.string.payment)
        object PayWithSetup : Checkout(R.string.payment_with_setup)
        object Setup : Checkout(R.string.setup)
    }

    sealed class Currency(@StringRes id: Int) : LabelIdButton(id) {
        object USD : Currency(R.string.currency_usd)
        object EUR : Currency(R.string.currency_eur)
    }

    sealed class GooglePayState(@IntegerRes id: Int) : IdButton(id) {
        object On : GooglePayState(R.id.google_pay_on_button)
        object Off : GooglePayState(R.id.google_pay_off_button)
    }

    sealed class Customer(@StringRes id: Int) : LabelIdButton(id) {
        object Guest : Customer(R.string.customer_guest)
        object New : Customer(R.string.customer_new)
        object Returning : Customer(R.string.customer_returning)
    }
}
