package com.stripe.android.test.core

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.os.Bundle
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.screenshot.Screenshot
import androidx.test.uiautomator.UiDevice
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.result.Result
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.gson.Gson
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.playground.activity.PaymentSheetPlaygroundActivity
import com.stripe.android.paymentsheet.example.playground.model.CheckoutCurrency
import com.stripe.android.paymentsheet.example.playground.model.CheckoutCustomer
import com.stripe.android.paymentsheet.example.playground.model.CheckoutMode
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.CheckoutResponse
import com.stripe.android.paymentsheet.viewmodels.MultiStepContinueIdlingResource
import com.stripe.android.paymentsheet.viewmodels.TransitionFragmentResource
import com.stripe.android.test.core.ui.BrowserUI
import com.stripe.android.test.core.ui.EspressoText
import com.stripe.android.test.core.ui.Selectors
import org.junit.Assume
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * This drives the end to end payment sheet flow for any set of
 * [TestParameters].  It handles any authorization that needs to happen
 * It drives the test from the test playground so that a payment
 * intent does not need to be created.
 *
 * This does not yet work when the locale is not english.
 * It works for all screen sizes
 * It does not test every possible drop down parameter
 */
class PlaygroundTestDriver(
    private val device: UiDevice,
    private val composeTestRule: ComposeTestRule,
    private val basicScreenCaptureProcessor: MyScreenCaptureProcessor,
) {
    private var resultValue: String? = null
    private lateinit var testParameters: TestParameters
    private lateinit var selectors: Selectors

    private val currentActivity = Array<Activity?>(1) { null }
    private var application: Application? = null

    private val activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {
            currentActivity[0] = activity
        }
    }

    fun confirmCustom(
        testParameters: TestParameters,
        populateCustomLpmFields: () -> Unit = {}
    ) {
        setup(testParameters)
        launchCustom()

        selectors.paymentSelection.click()

        FieldPopulator(
            selectors,
            testParameters,
            populateCustomLpmFields
        ).populateFields()

        Espresso.onIdle()
        composeTestRule.waitForIdle()
        takeScreenShot("first", true)
        val populatedFieldsScreenshot = getScreenshotBytes()

        pressContinue()

        pressMultiStepSelect()

        takeScreenShot("second", true)
        assertWithMessage("Screenshots differ").that(getScreenshotBytes())
            .isEqualTo(populatedFieldsScreenshot)

        teardown()
    }

    private fun pressMultiStepSelect() {
        selectors.multiStepSelect.click()
        waitForNotPlaygroundActivity()
    }

    private fun pressContinue() {
        selectors.continueButton.apply {
            scrollTo()
            click()
        }

        waitForPlaygroundActivity()
    }

    /**
     * This will open the payment sheet complete flow from the playground with a new or
     * guest user and complete the confirmation including any browser interactions.
     *
     * A test calling this takes about 25s on average to run.
     */
    fun confirmNewOrGuestComplete(
        testParameters: TestParameters,
        populateCustomLpmFields: () -> Unit = {}
    ) {
        setup(testParameters)
        launchComplete()

        selectors.paymentSelection.click()

        // This takes a screenshot so that translation strings of placeholders
        // and labels and design can all be verified
        takeScreenShot(
            fileName = "${selectors.baseScreenshotFilenamePrefix}-beforeText",
            testParameters.takeScreenshotOnLpmLoad
        )

        FieldPopulator(
            selectors,
            testParameters,
            populateCustomLpmFields
        ).populateFields()

        // This takes a screenshot so that design and style can be verified after
        // user input is entered.
        takeScreenShot(
            fileName = "${selectors.baseScreenshotFilenamePrefix}-afterText",
            testParameters.takeScreenshotOnLpmLoad
        )

        // Verify device requirements are met prior to attempting confirmation.  Do this
        // after we have had the chance to capture a screenshot.
        verifyDeviceSupportsTestAuthorization(
            testParameters.authorizationAction,
            testParameters.useBrowser
        )

        pressBuy()

        doAuthorization()

        teardown()
    }

    private fun pressBuy(){
        selectors.buyButton.apply {
            scrollTo()
            click()
        }
    }

    /**
     * Here we wait for an activity different from the playground to be in view.  We
     * don't specifically look for PaymentSheetActivity or PaymentOptionsActivity because
     * that would require exposing the activities publicly.
     */
    private fun waitForNotPlaygroundActivity() {
        while (currentActivity[0] is PaymentSheetPlaygroundActivity) {
            TimeUnit.MILLISECONDS.sleep(250)
        }
    }

    /**
     * Here we wait for the Playground to come back into view.
     */
    private fun waitForPlaygroundActivity() {
        while (currentActivity[0] !is PaymentSheetPlaygroundActivity) {
            TimeUnit.MILLISECONDS.sleep(250)
        }
    }

    private fun verifyDeviceSupportsTestAuthorization(
        authorizeAction: AuthorizeAction?,
        requestedBrowser: Browser?
    ) {
        authorizeAction?.let {
            requestedBrowser?.let {
                val browserUI = BrowserUI.convert(it)
                Assume.assumeTrue(getBrowser(browserUI) == browserUI)
            } ?: Assume.assumeTrue(selectors.getInstalledBrowsers().isNotEmpty())
        }
    }

    private fun getBrowser(requestedBrowser: BrowserUI?): BrowserUI {
        val installedBrowsers = selectors.getInstalledBrowsers()

        return requestedBrowser?.let {
            // Assume true will mark the test as skipped if it can't be executed
            Assume.assumeTrue(installedBrowsers.contains(it))
            it
        } ?: installedBrowsers.first()
    }

    private fun registerListeners() {
        val launchPlayground = Semaphore(1)
        launchPlayground.acquire()
        // Setup the playground for scenario, and launch it.  We use the playground
        // so we don't have to implement another route to create a payment intent,
        // the challenge is that we don't have access to the activity or it's viewmodels
        val scenario = ActivityScenario.launch(PaymentSheetPlaygroundActivity::class.java)
        scenario.onActivity { activity ->

            monitorCurrentActivity(activity.application)

            IdlingPolicies.setIdlingResourceTimeout(45, TimeUnit.SECONDS)
            IdlingPolicies.setMasterPolicyTimeout(45, TimeUnit.SECONDS)

            IdlingRegistry.getInstance().register(
                activity.getMultiStepReadyIdlingResource(),
                activity.getSingleStepReadyIdlingResource(),
                TransitionFragmentResource.getSingleStepIdlingResource(),
                MultiStepContinueIdlingResource.getSingleStepIdlingResource()
            )

            // Observe the result of the PaymentSheet completion
            activity.viewModel.status.observeForever {
                resultValue = it

                IdlingRegistry.getInstance().unregister(
                    activity.getMultiStepReadyIdlingResource(),
                    activity.getSingleStepReadyIdlingResource(),
                    TransitionFragmentResource.getSingleStepIdlingResource(),
                    MultiStepContinueIdlingResource.getSingleStepIdlingResource()
                )
            }
            launchPlayground.release()
        }

        launchPlayground.acquire()
        launchPlayground.release()
    }

    private fun monitorCurrentActivity(application: Application) {
        this.application = application
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }

    private fun setConfiguration(selectors: Selectors) {
        // Could consider setting these preferences instead of clicking
        // if it is faster (possibly 1-2s)
        selectors.customer.click()
        selectors.currency.click()
        selectors.checkout.click()
        selectors.delayed.click()

        // billing is not saved to preferences
        selectors.billing.click()

        // Can't guarantee that google pay will be on the phone
        selectors.googlePayState.click()

    }

    internal fun launchComplete() {
        selectors.reload.click()
        selectors.complete.click()

        // PaymentSheetActivity is now on screen
        waitForNotPlaygroundActivity()
    }

    private fun launchCustom() {
        selectors.reload.click()
        selectors.multiStepSelect.click()

        // PaymentOptionsActivity is now on screen
        waitForNotPlaygroundActivity()
    }

    private fun doAuthorization() {
        selectors.apply {
            if (testParameters.authorizationAction != null
                && this.authorizeAction != null
            ) {
                // If a specific browser is requested we will use it, otherwise, we will
                // select the first browser found
                val selectedBrowser = getBrowser(BrowserUI.convert(testParameters.useBrowser))

                // If there are multiple browser there is a browser selector window
                selectBrowserPrompt.wait(2000)
                if (selectBrowserPrompt.exists()) {
                    browserIconAtPrompt(selectedBrowser).click()
                }

                assertThat(browserWindow(selectedBrowser)?.exists()).isTrue()

                blockUntilAuthorizationPageLoaded()

                authorizeAction.click()

                when (testParameters.authorizationAction) {
                    AuthorizeAction.Authorize -> {}
                    AuthorizeAction.Cancel -> {
                        buyButton.apply {
                            waitProcessingComplete()
                            isEnabled()
                            isDisplayed()
                        }
                    }
                    AuthorizeAction.Fail -> {
                        buyButton.apply {
                            waitProcessingComplete()
                            isEnabled()
                            isDisplayed()
                        }

                        // The text comes after the buy button animation is complete
                        // TODO: This string gets localized.
                        EspressoText(
                            "We are unable to authenticate your payment method. Please " +
                                "choose a different payment method and try again."
                        ).isDisplayed()
                    }
                    null -> {}
                }
            } else {
                // Make sure there is no prompt and no browser window open
                assertThat(selectBrowserPrompt.exists()).isFalse()
                BrowserUI.values().forEach {
                    assertThat(Selectors.browserWindow(device, it)?.exists() == true).isFalse()
                }
            }

        }

        if (testParameters.authorizationAction == AuthorizeAction.Authorize
            || testParameters.authorizationAction == null
        ) {
            waitForPlaygroundActivity()
            assertThat(resultValue).isEqualTo(
                PaymentSheetResult.Completed.toString()
            )
        }
    }

    private fun getScreenshotBytes(): ByteArray {
        val byteScreenCaptureProcessor = ByteScreenCaptureProcessor()

        val capture = Screenshot.capture()
        capture.process(setOf(byteScreenCaptureProcessor))

        return byteScreenCaptureProcessor.getBytes()
    }

    private fun takeScreenShot(
        fileName: String,
        takeScreenshotOnLpmLoad: Boolean
    ) {
        if (takeScreenshotOnLpmLoad) {
            val capture = Screenshot.capture()
            capture.name = fileName
            capture.format = Bitmap.CompressFormat.PNG

            capture.process(setOf(basicScreenCaptureProcessor))
        }
    }

    internal fun setup(testParameters: TestParameters) {
        this.testParameters = testParameters
        this.selectors = Selectors(device, composeTestRule, testParameters)

        registerListeners()
        setConfiguration(selectors)
    }

    internal fun teardown() {
        application?.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }

    var customerConfig: PaymentSheet.CustomerConfiguration? = null
    var clientSecret: String? = null
    var checkoutMode: CheckoutMode? = null
    var temporaryCustomerId: String? = null

    fun processCheckoutRequest() {
        val backendUrl = "https://mature-buttery-bumper.glitch.me/"
        val mode = CheckoutMode.Payment
        val customer = CheckoutCustomer.New
        val requestBody = CheckoutRequest(
            customer.value,
            CheckoutCurrency.USD.value,
            mode.value,
            false,
            false,
            false
        )

        val httpAsync = Fuel.post(backendUrl + "checkout")
            .jsonBody(Gson().toJson(requestBody))
            .responseString { _, _, result ->
                when (result) {
                    is Result.Failure -> {
                        AssertionError("Failed to call checkout")
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
                        PaymentConfiguration.init(
                            InstrumentationRegistry.getInstrumentation().targetContext.applicationContext,
                            checkoutResponse.publishableKey
                        )

                        customerConfig = checkoutResponse.makeCustomerConfig()
                        clientSecret = checkoutResponse.intentClientSecret
                    }
                }
            }

        httpAsync.join()
    }
}
