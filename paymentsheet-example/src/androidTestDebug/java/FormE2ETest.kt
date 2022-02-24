import android.os.Environment.DIRECTORY_PICTURES
import android.util.Log
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.runner.screenshot.BasicScreenCaptureProcessor
import androidx.test.runner.screenshot.Screenshot
import androidx.test.uiautomator.UiDevice
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.result.Result
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.playground.activity.PaymentSheetPlaygroundActivity
import com.stripe.android.paymentsheet.example.playground.activity.PaymentSheetPlaygroundActivity.Companion.multiStepUIIdlingResource
import com.stripe.android.paymentsheet.example.playground.activity.PaymentSheetPlaygroundActivity.Companion.singleStepUIIdlingResource
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.CheckoutResponse
import com.stripe.android.paymentsheet.transitionFragmentResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.InvalidParameterException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@RunWith(AndroidJUnit4::class)
internal class FormE2ETest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    val inProgress = MutableLiveData<Boolean>()

    private lateinit var device: UiDevice
    private val screenshotProcessor = setOf(MyScreenCaptureProcessor())

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

    @ExperimentalCoroutinesApi
    @Test
    fun testAllCurrencyAndModesAccepted() = runBlocking {
        generateTestParameters { testParameters, paymentSelection ->
            try {
                confirmComplete(testParameters) {
                    print("Testing: $testParameters, paymentSelection: $paymentSelection")
                    if (paymentSelection.exists()) {
                        paymentSelection.click(getInstrumentation().targetContext.resources)
                        // Fill in form values (SEPA needs IBAN and all card fields)
                        if (paymentSelection is PaymentSelection.SepaDebit) {
                            composeTestRule.onNodeWithText("IBAN")
                                .performTextInput("DE89370400440532013000")
                        }

                        if (SaveForFutureCheckbox.exists()) {
                            print("... save for future use not visible")
                        } else if (SaveForFutureCheckbox.exists() && !testParameters.saveCheckboxValue) {
                            SaveForFutureCheckbox.click()
                        }

                        // Validate the state of the save checkbox
                    } else {
                        throw InvalidParameterException("... paymentSelection: $paymentSelection is not a valid test setup")
                    }

                }
            } catch (e: InvalidParameterException) {
                print(e)
            }

            println("")
        }
    }

    @Test
    fun testAllCurrencyAndModes() = runBlocking {
        generateTestParameters { testParameters, paymentSelection ->
            paymentSelection.click(getInstrumentation().targetContext.resources)

            closeSoftKeyboard()

            val capture = Screenshot.capture()
            capture.name =
                "${testParameters.checkout.javaClass.name}.${getResourceString(paymentSelection.label)}"
            capture.process(screenshotProcessor)

            // TODO: Need to add scrolling
        }
    }
//
//    @Test
//    fun returningCustomerComplete() {
//        confirmComplete(
//            TestParameters(
//                Customer.Returning,
//                GooglePayState.Off,
//                Currency.EUR,
//                Checkout.Pay,
//                Billing.On,
//                false
//            ),
//        ) {
//            try {
//                onView(withId(com.stripe.android.paymentsheet.R.id.recycler))
//                    .check(matches(isDisplayed()))
//                    .perform(
//                        RecyclerViewActions.actionOnHolderItem(
//                            matchFirstPaymentOptionsHolder("4242"),
//                            click()
//                        )
//                    )
//            } catch (e: PerformException) {
//                // Item is already selected.
//            }
//        }
//    }

    private fun getResourceString(id: Int) =
        getInstrumentation().targetContext.resources.getString(id)

    private fun launchCompleteScenario(
        testParameters: TestParameters,
        onActivity: (PaymentSheetPlaygroundActivity) -> Unit
    ) {
        val scenario = launch(PaymentSheetPlaygroundActivity::class.java)
        scenario.onActivity { activity ->
            onActivity(activity as PaymentSheetPlaygroundActivity)
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
    }

    /**
     * This one doesn't care if it is a new or returning customer
     */
    private fun confirmComplete(testParameters: TestParameters, logic: () -> Unit) {
        // TODO: I can imagine a different test that checks the different authorization states.
        val authorization = AuthorizationParameters(
            Browser.Chrome,
            AuthorizeAction.Authorize
        )
        var resultValue: String? = null
        val callbackLock = Semaphore(1)

        launchCompleteScenario(testParameters) { activity ->
            callbackLock.acquire()
            activity.viewModel.status.observeForever {
                resultValue = it
                callbackLock.release()
            }
        }

        logic()

        onView(withId(R.id.buy_button))
            .perform(ViewActions.scrollTo())
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

    private fun generateTestParameters(yield: (TestParameters, PaymentSelection) -> Unit) =
        runBlocking {
            listOf(Currency.EUR, Currency.USD).forEach { currency ->
                listOf(
                    Checkout.Pay,
                    Checkout.PayWithSetup,
                    Checkout.Setup
                ).forEach { checkout ->
                    listOf(Customer.New, Customer.Guest).forEach { customer ->

                        listOf(true, false).forEach { saveCheckboxValue ->
                            val testParameters = TestParameters(
                                customer,
                                GooglePayState.Off,
                                currency,
                                checkout,
                                Billing.On,
                                saveCheckboxValue
                            )

                            val checkoutResponse = checkoutSessionSetup(testParameters)
                            checkoutResponse?.intentLpms?.let { intentPaymentMethodTypes ->
                                intentPaymentMethodTypes
                                    // acss_debit not supported, not able to fill in card fields
                                    .filterNot { it == "acss_debit" || it == "card" || it == "sepa_debit" }
                                    .map { lpmStr ->
                                        PaymentSelection.fromString(lpmStr)
                                    }.forEach {
                                        yield(testParameters, it)
                                    }
                            } ?: throw RuntimeException("Unable to create Intent")
                        }
                    }
                }
            }
        }

    private suspend fun checkoutSessionSetup(testParameters: TestParameters): CheckoutResponse? {
        val requestBody = CheckoutRequest(
            testParameters.customer.toRepository().value,
            testParameters.currency.toRepository().value,
            testParameters.checkout.toRepository().value,
            set_shipping_address = false,
            automatic_payment_methods = false
        )

        return suspendCoroutine { continuation ->
            Fuel.post("https://innate-lofty-napkin.glitch.me/checkout")
                .jsonBody(Gson().toJson(requestBody))
                .responseString { _, _, result ->
                    when (result) {
                        is Result.Failure -> {
                            continuation.resume(null)
                        }
                        is Result.Success -> {
                            val checkoutResponse =
                                Gson().fromJson(result.get(), CheckoutResponse::class.java)

                            // Init PaymentConfiguration with the publishable key returned from the backend,
                            // which will be used on all Stripe API calls
                            PaymentConfiguration.init(
                                getInstrumentation().targetContext,
                                checkoutResponse.publishableKey
                            )

                            continuation.resume(checkoutResponse)
                        }
                    }
                    inProgress.postValue(false)
                }
        }

    }

    class MyScreenCaptureProcessor : BasicScreenCaptureProcessor() {

        init {
            val pattern = "yyyy-MM-dd-HH-mm-ss"
            val simpleDateFormat = SimpleDateFormat(pattern)
            val date: String = simpleDateFormat.format(Date())

            this.mDefaultScreenshotPath = File(
                File(
                    getInstrumentation().targetContext.getExternalFilesDir(
                        DIRECTORY_PICTURES
                    ),
                    "payment_sheet_instrumentation_tests",
                ).absolutePath,
                "screenshots/$date/"
            )
            Log.d("STRIPE", mDefaultScreenshotPath.absolutePath)
        }

        override fun getFilename(prefix: String): String = prefix
    }

    data class TestParameters(
        val customer: Customer,
        val googlePayState: GooglePayState,
        val currency: Currency,
        val checkout: Checkout,
        val billing: Billing,
        val saveCheckboxValue: Boolean,
    )

    data class AuthorizationParameters(
        val browser: Browser,
        val action: AuthorizeAction
    )
}
