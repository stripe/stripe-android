import android.os.Environment.DIRECTORY_PICTURES
import android.util.Log
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
import androidx.test.runner.screenshot.BasicScreenCaptureProcessor
import androidx.test.runner.screenshot.Screenshot
import androidx.test.uiautomator.UiDevice
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.result.Result
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.stripe.android.ApiResultCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.playground.activity.PaymentSheetPlaygroundActivity.Companion.multiStepUIIdlingResource
import com.stripe.android.paymentsheet.example.playground.activity.PaymentSheetPlaygroundActivity.Companion.singleStepUIIdlingResource
import com.stripe.android.paymentsheet.example.playground.activity.PaymentSheetPlaygroundActivity
import com.stripe.android.paymentsheet.example.playground.model.CheckoutCustomer
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.CheckoutResponse
import com.stripe.android.paymentsheet.matchFirstPaymentOptionsHolder
import com.stripe.android.paymentsheet.transitionFragmentResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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

    private lateinit var device: UiDevice
    private val screenshotProcessor = setOf(MyScreenCaptureProcessor())

    @Before
    fun before() {
        androidx.test.espresso.intent.Intents.init()
        device = UiDevice.getInstance(getInstrumentation())
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

    @Test
    fun returningCustomerComplete() {
        confirmComplete(
            TestParameters(
                Customer.Returning,
                GooglePayState.Off,
                Currency.EUR,
                Checkout.Pay,
                Billing.On,
                false
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

                            retrievePaymentIntent(testParameters)?.let { intent ->
                                intent.paymentMethodTypes
                                    // acss_debit not supported, not able to fill in card fields
                                    .filterNot { it == "acss_debit" || it == "card" || it == "sepa_debit" }
                                    .map { lpmStr ->
                                        PaymentSelection.fromString(lpmStr)
                                    }.forEach {
                                        yield(testParameters, it)
                                    }
                            }
                        }
                    }
                }
            }
        }

    private suspend fun retrievePaymentIntent(testParameters: TestParameters): StripeIntent? {
        val requestBody = CheckoutRequest(
            testParameters.customer.toRepository(),
            testParameters.currency.toRepository(),
            testParameters.checkout.toRepository(),
            false,
            false
        )

        Fuel.post("https://stripe-mobile-payment-sheet-test-playground-v6.glitch.me/checkout")
            .jsonBody(Gson().toJson(requestBody))
            .responseString { _, _, result ->
                when (result) {
                    is Result.Failure -> {
                        status.postValue(
                            "Preparing checkout failed:\n${result.getException().message}"
                        )
                    }
                    is Result.Success -> {
                        val checkoutResponse = Gson()
                            .fromJson(result.get(), CheckoutResponse::class.java)
                        checkoutMode = mode
                        temporaryCustomerId = if (customer == CheckoutCustomer.New) {
                            checkoutResponse.customerId
                        } else {
                            null
                        }

                        // Init PaymentConfiguration with the publishable key returned from the backend,
                        // which will be used on all Stripe API calls
                        PaymentConfiguration.init(getApplication(), checkoutResponse.publishableKey)

                        customerConfig.postValue(checkoutResponse.makeCustomerConfig())
                        clientSecret.postValue(checkoutResponse.intentClientSecret)
                    }
                }
                inProgress.postValue(false)
            }


        val stripe = Stripe(getInstrumentation().targetContext, response.publishableKey)

        return suspendCoroutine { cont ->
            when (testParameters.checkout) {
                is Checkout.Pay, is Checkout.PayWithSetup ->
                    stripe.retrievePaymentIntent(
                        response.intentClientSecret,
                        null,
                        object : ApiResultCallback<PaymentIntent> {
                            override fun onSuccess(result: PaymentIntent) {
                                cont.resume(result)
                            }

                            override fun onError(e: Exception) {
                                cont.resume(null)
                            }
                        }
                    )
                else -> {
                    stripe.retrieveSetupIntent(
                        response.intentClientSecret,
                        null,
                        object : ApiResultCallback<SetupIntent> {
                            override fun onSuccess(result: SetupIntent) {
                                cont.resume(result)
                            }

                            override fun onError(e: Exception) {
                                cont.resume(null)
                            }
                        }
                    )
                }
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
