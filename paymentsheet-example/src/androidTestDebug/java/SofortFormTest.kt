import android.util.Log
import android.view.View
import androidx.annotation.IntegerRes
import androidx.annotation.StringRes
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.util.HumanReadables
import androidx.test.espresso.util.TreeIterables
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.paymentsheet.PaymentSheetActivity
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.activity.PaymentSheetPlaygroundActivity
import com.stripe.android.paymentsheet.example.activity.PaymentSheetPlaygroundActivity.Companion.multiStepUIIdlingResource
import com.stripe.android.paymentsheet.example.activity.PaymentSheetPlaygroundActivity.Companion.singleStepUIIdlingResource
import com.stripe.android.paymentsheet.example.activity.Playground
import com.stripe.android.paymentsheet.example.viewmodel.PaymentSheetPlaygroundViewModel
import com.stripe.android.paymentsheet.matchFirstPaymentOptionsHolder
import com.stripe.android.paymentsheet.ui.PrimaryButton
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


@RunWith(AndroidJUnit4::class)
internal class SofortFormTest {
    val composeTestRule = createEmptyComposeRule()

//    val activityRule = ActivityScenarioRule(PaymentSheetPlaygroundActivity::class.java)

//    @get:Rule
//    val rule = RuleChain.outerRule(activityRule).around(composeTestRule)
    private val idleResources = listOf(
        singleStepUIIdlingResource,
        multiStepUIIdlingResource,
    PrimaryButton.buyButtonIdleResource,
    PaymentSheetActivity.transitionFragmentResource
//        paymentSheetResponse
    )

    //        IdlingPolicies.setIdlingResourceTimeout(40, TimeUnit.SECONDS)
//        onView(withText("PLAYGROUND")).perform(click())
//        onView(withText(R.string.playground)).check(matches(isDisplayed()))

    @Before
    fun before() {
        androidx.test.espresso.intent.Intents.init()
        // To prove that the test fails, omit this call:
        idleResources
            .forEach { IdlingRegistry.getInstance().register(it) }
    }

    @After
    fun after() {
        idleResources.forEach { IdlingRegistry.getInstance().unregister(it) }
    }

    // TODO: Need to revist the idle resource construction - should probably be dependency injected

    @Test
    fun ReturningCustomer() {
        val scenario = launch(PaymentSheetPlaygroundActivity::class.java)
        var playgroundActivity: PaymentSheetPlaygroundActivity? = null
        scenario.onActivity { activity ->
            playgroundActivity = (activity as PaymentSheetPlaygroundActivity)
//            (activity as PaymentSheetPlaygroundActivity).viewModel.status.observeForever{
//                Log.e("STRIPE", "GOT THE STRING!!! $it ***")
//            }
        }

        Customer.Returning.click()
        GooglePayState.Off.click()
        Currency.EUR.click() // If USD already selected it gives an error
        Checkout.Pay.click()

        LabelIdButton(R.string.reload_paymentsheet).click()

        onView(withText(R.string.checkout_complete)).perform(click())

        Espresso.onIdle()
//            .perform(waitUntilShown(R.id.recycler, 10000))
            // TODO: Make sure animations are off
            // Wait for on create to complete
            // sleep
            // Start idle press buy

            // If it is already selected this does not work
            onView(withId(com.stripe.android.paymentsheet.R.id.recycler))
                .check(matches(isDisplayed()))
                .perform(
                    RecyclerViewActions.actionOnHolderItem(
                        matchFirstPaymentOptionsHolder("4242"),
                        click()
                    )
                )

        Log.e("STRIPE", "State: ${scenario.state}")
            Log.e("STRIPE", "perform buy click")
            onView(withId(R.id.buy_button)).perform(click())
//        paymentSheetResponse.increment()

//            TimeUnit.SECONDS.sleep(1)
//        onView(isRoot()).perform(waitUntilShown(R.id.result, 10000))

            Log.e("STRIPE", "look for result")
            onView(withId(R.id.result))
                .check(
                    matches(
                        withText(
                            "Completed"
                        )
                    )
                )
    }

    /**
     * Perform action of waiting until the element is accessible & not shown.
     * @param viewId The id of the view to wait for.
     * @param millis The timeout of until when to wait for.
     */
    fun waitUntilShown(viewId: Int, millis: Long): ViewAction? {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isRoot()
            }

            override fun getDescription(): String {
                return "wait for a specific view with id <$viewId> is shown during $millis millis."
            }

            override fun perform(uiController: UiController, view: View?) {
                uiController.loopMainThreadUntilIdle()
                val startTime = System.currentTimeMillis()
                val endTime = startTime + millis
                val viewMatcher: Matcher<View> = withId(viewId)
                do {
                    for (child in TreeIterables.breadthFirstViewTraversal(view)) {
                        // found view with required ID
                        if (viewMatcher.matches(child) && child.isShown) {
                            return
                        }
                    }
                    uiController.loopMainThreadForAtLeast(50)
                } while (System.currentTimeMillis() < endTime)
                throw PerformException.Builder()
                    .withActionDescription(this.description)
                    .withViewDescription(HumanReadables.describe(view))
                    .withCause(TimeoutException())
                    .build()
            }
        }
    }

    @Test
    fun SofortFormTest() {

        Customer.Returning.click()
        GooglePayState.Off.click()
        Currency.EUR.click() // If USD already selected it gives an error
        Checkout.Pay.click()
//        Billing.On.click()

        LabelIdButton(R.string.reload_paymentsheet).click()
        onView(withText(R.string.checkout_complete)).perform(click())

        // TODO: Need to revist the idle resource construction - should probably be dependency injected
//
//        TimeUnit.SECONDS.sleep(1)
//
        onView(withId(R.id.buy_button)).perform(click())
//        TimeUnit.SECONDS.sleep(1)
//
//        onView(withId(R.id.payment_methods_recycler))
//            .check(matches(isDisplayed()))
//            .perform(
//                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
//                    hasDescendant(
//                        withText(
//                            PaymentSelection.Sofort.label
//                        )
//                    )
//                )
//            )
//
//        PaymentSelection.Sofort.click()
//        TimeUnit.SECONDS.sleep(5)
//
//        onView(withId(R.id.payment_methods_recycler))
//            .check(matches(isDisplayed()))
//            .perform(
//                RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(0)
//            )
//        TimeUnit.SECONDS.sleep(1)
        PaymentSelection.iDEAL.click()
//        TimeUnit.SECONDS.sleep(5)

        composeTestRule.onNodeWithText("Name").performTextInput("My name")
        composeTestRule.onNodeWithText("Email").performTextInput("email@email.com")

//        TimeUnit.SECONDS.sleep(1)
        onView(withId(R.id.buy_button)).perform(click())

//
//        PaymentSelection.Sofort.click()
//        TimeUnit.SECONDS.sleep(5)
//        PaymentSelection.SepaDebit.click()
//        TimeUnit.SECONDS.sleep(5)

    }


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

    sealed class PaymentSelection {
        object Card : LabelIdButton(R.string.stripe_paymentsheet_payment_method_card)
        object Sofort : LabelIdButton(R.string.stripe_paymentsheet_payment_method_sofort)
        object iDEAL : LabelIdButton(R.string.stripe_paymentsheet_payment_method_ideal)
        object Bancontact : LabelIdButton(R.string.stripe_paymentsheet_payment_method_bancontact)
        object SepaDebit : LabelIdButton(R.string.stripe_paymentsheet_payment_method_sepa_debit)
    }

    sealed class Billing {
        object On : IdButton(R.id.default_billing_on_button)
        object Off : IdButton(R.id.default_billing_off_button)
    }

    sealed class Checkout {
        object Pay : LabelIdButton(R.string.payment)
        object PayWithSetup : LabelIdButton(R.string.payment_with_setup)
        object Setup : LabelIdButton(R.string.setup)
    }

    sealed class Currency {
        object USD : LabelIdButton(R.string.currency_usd)
        object EUR : LabelIdButton(R.string.currency_eur)
    }

    sealed class GooglePayState {
        object On : IdButton(R.id.google_pay_on_button)
        object Off : IdButton(R.id.google_pay_off_button)
    }

    sealed class Customer {
        object Guest : LabelIdButton(R.string.customer_guest)
        object New : LabelIdButton(R.string.customer_new)
        object Returning : LabelIdButton(R.string.customer_returning)
    }

}
