package com.stripe.android.test.core

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.IdlingPolicies
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import com.karumi.shot.ScreenshotTest
import com.stripe.android.paymentsheet.PAYMENT_OPTION_CARD_TEST_TAG
import com.stripe.android.paymentsheet.example.playground.activity.PaymentSheetPlaygroundActivity
import com.stripe.android.test.core.ui.BrowserUI
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

    fun testLinkCustom(
        testParameters: TestParameters,
        populateCustomLpmFields: () -> Unit = {},
        verifyCustomLpmFields: () -> Unit = {}
    ) {
        setup(testParameters)
        launchCustom()

        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 5000L) {
            selectors.addPaymentMethodButton.isDisplayed()
        }

        composeTestRule.onNodeWithTag("${PAYMENT_OPTION_CARD_TEST_TAG}_+ Add").apply {
            assertExists()
            performClick()
        }

        val fieldPopulator = FieldPopulator(
            selectors,
            testParameters,
            populateCustomLpmFields,
            verifyCustomLpmFields
        )
        fieldPopulator.populateFields()

        composeTestRule.onNodeWithText("Save my info for secure 1-click checkout").apply {
            assertExists()
            performClick()
        }

        composeTestRule.onNodeWithText("Email").apply {
            assertExists()
            performTextInput("email@email.com")
        }

        closeSoftKeyboard()

        runCatching {
            // We need to wait for the built in debounce time for filling in the link email.
            composeTestRule.waitUntil(timeoutMillis = 1100L) {
                false
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5000L) {
            selectors.continueButton.checkEnabled()
        }

        composeTestRule.waitForIdle()

        selectors.continueButton.click()

        composeTestRule.waitUntil(timeoutMillis = 5000L) {
            composeTestRule.onAllNodesWithTag("OTP-0").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("OTP-0").performTextInput("123456")

        Espresso.onIdle()
        composeTestRule.waitForIdle()

        waitForPlaygroundActivity()

        selectors.multiStepSelect.click()

        waitForNotPlaygroundActivity()

        composeTestRule.waitUntil(timeoutMillis = 5000L) {
            composeTestRule.onAllNodesWithTag("SignedInBox").fetchSemanticsNodes().isNotEmpty()
        }

        Espresso.onIdle()
        composeTestRule.waitForIdle()

        fieldPopulator.verifyFields()

        teardown()
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
        selectors.continueButton.click()
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

        FieldPopulator(
            selectors,
            testParameters,
            populateCustomLpmFields,
        ).populateFields()

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

        composeTestRule.waitForIdle()
        device.waitForIdle()

        customOperations()

        currentActivity[0]?.let {
            compareScreenshot(it)
        }

        teardown()
    }

    private fun pressBuy() {
        selectors.buyButton.click()
    }

    internal fun pressEdit() {
        composeTestRule.waitUntil {
            composeTestRule
                .onAllNodesWithText("EDIT")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule
            .onNodeWithText("EDIT")
            .performClick()
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
        selectors.reset.click()
        // Could consider setting these preferences instead of clicking
        // if it is faster (possibly 1-2s)
        selectors.customer.click()
        selectors.automatic.click()

        selectors.setInitializationType(testParameters.initializationType)

        // Set the country first because it will update the default currency value
        selectors.setMerchantCountry(testParameters.merchantCountryCode)
        selectors.setCurrency(testParameters.currency)

        selectors.linkState.click()

        selectors.checkout.click()
        selectors.delayed.click()
        selectors.shipping.click()

        // billing is not saved to preferences
        selectors.billing.click()

        // billing is not saved to preferences
        selectors.shipping.click()

        // Can't guarantee that google pay will be on the phone
        selectors.googlePayState.click()

        // Billing details collection.
        selectors.attachDefaults.click()
        selectors.collectName.click()
        selectors.collectEmail.click()
        selectors.collectPhone.click()
        selectors.collectAddress.click()

        testParameters.customPrimaryButtonLabel?.let { customLabel ->
            selectors.enterCustomPrimaryButtonLabel(text = customLabel)
        }
    }

    internal fun launchComplete() {
        selectors.reload.click()
        selectors.complete.waitForEnabled()
        selectors.complete.click()

        // PaymentSheetActivity is now on screen
        waitForNotPlaygroundActivity()
    }

    private fun launchCustom() {
        selectors.reload.click()
        Espresso.onIdle()
        selectors.composeTestRule.waitForIdle()

        selectors.multiStepSelect.waitForEnabled()
        selectors.multiStepSelect.click()

        // PaymentOptionsActivity is now on screen
        waitForNotPlaygroundActivity()
    }

    private fun doAuthorization() {
        selectors.apply {
            if (testParameters.authorizationAction != null && authorizeAction != null) {
                // If a specific browser is requested we will use it, otherwise, we will
                // select the first browser found
                val selectedBrowser = getBrowser(BrowserUI.convert(testParameters.useBrowser))

                // If there are multiple browser there is a browser selector window
                selectBrowserPrompt.wait(4000)
                if (selectBrowserPrompt.exists()) {
                    browserIconAtPrompt(selectedBrowser).click()
                }

                assertThat(browserWindow(selectedBrowser)?.exists()).isTrue()

                blockUntilAuthorizationPageLoaded()

                if (authorizeAction.exists()) {
                    authorizeAction.click()
                } else if (!authorizeAction.exists()) {
                // Buttons aren't showing the same way each time in the web page.
                    object : UiAutomatorText(
                        label = requireNotNull(testParameters.authorizationAction).text,
                        className = "android.widget.TextView",
                        device = device
                    ) {}.click()
                    Log.e("Stripe", "Fail authorization was a text view not a button this time")
                }

                when (val authAction = testParameters.authorizationAction) {
                    is AuthorizeAction.Authorize -> {}
                    is AuthorizeAction.Cancel -> {
                        buyButton.apply {
                            waitProcessingComplete()
                            isEnabled()
                            isDisplayed()
                        }
                    }
                    is AuthorizeAction.Fail -> {
                        buyButton.apply {
                            waitProcessingComplete()
                            isEnabled()
                            isDisplayed()
                        }

                        // The text comes after the buy button animation is complete
                        composeTestRule.waitUntil {
                            runCatching {
                                composeTestRule
                                    .onNodeWithText(authAction.expectedError)
                                    .assertIsDisplayed()
                            }.isSuccess
                        }
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

        val isDone = testParameters.authorizationAction in setOf(AuthorizeAction.Authorize, null)

        if (isDone) {
            waitForPlaygroundActivity()
            assertThat(resultValue).isEqualTo("Success")
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
        intent.putExtra(
            PaymentSheetPlaygroundActivity.SUPPORTED_PAYMENT_METHODS_EXTRA,
            testParameters.supportedPaymentMethods.toTypedArray()
        )

        val scenario = ActivityScenario.launch<PaymentSheetPlaygroundActivity>(intent)
        scenario.onActivity { activity ->
            monitorCurrentActivity(activity.application)

            IdlingPolicies.setIdlingResourceTimeout(45, TimeUnit.SECONDS)
            IdlingPolicies.setMasterPolicyTimeout(45, TimeUnit.SECONDS)

            // Observe the result of the PaymentSheet completion
            activity.viewModel.status.observeForever {
                resultValue = it
            }
            launchPlayground.release()
        }

        launchPlayground.acquire()
        launchPlayground.release()

        closeSoftKeyboard()

        setConfiguration(selectors)
    }

    internal fun teardown() {
        application?.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }
}
