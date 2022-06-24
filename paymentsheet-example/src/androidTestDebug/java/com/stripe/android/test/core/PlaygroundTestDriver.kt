package com.stripe.android.test.core

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.runner.screenshot.Screenshot
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import com.karumi.shot.ScreenshotTest
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.playground.activity.PaymentSheetPlaygroundActivity
import com.stripe.android.test.core.ui.BrowserUI
import com.stripe.android.test.core.ui.EspressoText
import com.stripe.android.test.core.ui.Selectors
import com.stripe.android.test.core.ui.UiAutomatorText
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
) : ScreenshotTest {
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
        populateCustomLpmFields: () -> Unit = {},
        verifyCustomLpmFields: () -> Unit = {}
    ) {
        setup(testParameters)
        launchCustom()

        selectors.paymentSelection.click()

        val fieldPopulator = FieldPopulator(
            selectors,
            testParameters,
            populateCustomLpmFields,
            verifyCustomLpmFields
        )
        fieldPopulator.populateFields()

        Espresso.onIdle()
        composeTestRule.waitForIdle()

        pressContinue()

        pressMultiStepSelect()

        Espresso.onIdle()
        composeTestRule.waitForIdle()

        fieldPopulator.verifyFields()

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
            populateCustomLpmFields,
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

    /**
     * This test will open the payment sheet complete flow and take a picture when it has finished
     * opening. The sheet is then closed. We will use the screenshot to compare o a golden value
     * in our repository.
     *
     * A test calling this takes about 20 seconds
     */
    fun screenshotRegression(
        testParameters: TestParameters,
        customOperations: () -> Unit = {}
    ) {
        setup(testParameters)
        launchComplete()

        customOperations()

        currentActivity[0]?.let {
            compareScreenshot(it)
        }

        teardown()
    }

    private fun pressBuy() {
        selectors.buyButton.apply {
            scrollTo()
            click()
        }
    }

    internal fun pressEdit() {
        selectors.editButton.apply {
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
        Espresso.onIdle()
        composeTestRule.waitForIdle()
    }

    /**
     * Here we wait for the Playground to come back into view.
     */
    private fun waitForPlaygroundActivity() {
        while (currentActivity[0] !is PaymentSheetPlaygroundActivity) {
            TimeUnit.MILLISECONDS.sleep(250)
        }
        Espresso.onIdle()
        composeTestRule.waitForIdle()
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

    private fun monitorCurrentActivity(application: Application) {
        this.application = application
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }

    private fun setConfiguration(selectors: Selectors) {
        // Could consider setting these preferences instead of clicking
        // if it is faster (possibly 1-2s)
        selectors.customer.click()
        selectors.automatic.click()
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
        Espresso.onIdle()
        selectors.composeTestRule.waitForIdle()

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

                if(authorizeAction.exists()){
                    authorizeAction.click()
                }
                // Failure isn't showing the same way each time.
                else if(!authorizeAction.exists() && (testParameters.authorizationAction == AuthorizeAction.Fail)){
                    object : UiAutomatorText(
                        label = AuthorizeAction.Fail.text,
                        className = "android.widget.TextView",
                        device = device
                    ) {}.click()
                    Log.e("Stripe", "Fail authorization was a text view not a button this time")
                }
                // Failure isn't showing the same way each time.
                else if(!authorizeAction.exists() && (testParameters.authorizationAction == AuthorizeAction.Authorize)){
                    object : UiAutomatorText(
                        label = AuthorizeAction.Authorize.text,
                        className = "android.widget.TextView",
                        device = device
                    ) {}.click()
                    Log.e("Stripe", "Authorization was a text view not a button this time")
                }

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

        val launchPlayground = Semaphore(1)
        launchPlayground.acquire()

        // Setup the playground for scenario, and launch it.  We use the playground
        // so we don't have to implement another route to create a payment intent,
        // the challenge is that we don't have access to the activity or it's viewmodels
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            PaymentSheetPlaygroundActivity::class.java
        )
        intent.putExtra(
            PaymentSheetPlaygroundActivity.FORCE_DARK_MODE_EXTRA,
            testParameters.forceDarkMode
        )
        intent.putExtra(
            PaymentSheetPlaygroundActivity.APPEARANCE_EXTRA,
            testParameters.appearance
        )
        intent.putExtra(
            PaymentSheetPlaygroundActivity.USE_SNAPSHOT_RETURNING_CUSTOMER_EXTRA,
            testParameters.snapshotReturningCustomer
        )
        val scenario = ActivityScenario.launch<PaymentSheetPlaygroundActivity>(intent)
        scenario.onActivity { activity ->

            monitorCurrentActivity(activity.application)

            IdlingPolicies.setIdlingResourceTimeout(45, TimeUnit.SECONDS)
            IdlingPolicies.setMasterPolicyTimeout(45, TimeUnit.SECONDS)

            IdlingRegistry.getInstance().register(
                activity.getMultiStepReadyIdlingResource(),
                activity.getSingleStepReadyIdlingResource(),
            )

            // Observe the result of the PaymentSheet completion
            activity.viewModel.status.observeForever {
                resultValue = it

                IdlingRegistry.getInstance().unregister(
                    activity.getMultiStepReadyIdlingResource(),
                    activity.getSingleStepReadyIdlingResource(),
                )
            }
            launchPlayground.release()
        }

        launchPlayground.acquire()
        launchPlayground.release()

        setConfiguration(selectors)
    }

    internal fun teardown() {
        application?.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }
}
