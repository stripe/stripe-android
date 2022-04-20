package com.stripe.android.test.core

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.runner.screenshot.Screenshot
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.playground.activity.PaymentSheetPlaygroundActivity
import com.stripe.android.paymentsheet.viewmodels.TransitionFragmentResource
import com.stripe.android.test.core.ui.BrowserUI
import com.stripe.android.test.core.ui.EspressoIdButton
import com.stripe.android.test.core.ui.EspressoLabelIdButton
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
    private val paymentSheetFinishedLock = Semaphore(1)
    private lateinit var testParameters: TestParameters
    private lateinit var selectors: Selectors

    fun confirmCustom(
        testParameters: TestParameters,
        populateCustomLpmFields: () -> Unit = {}
    ) {
        this.selectors = Selectors(device, composeTestRule, testParameters)
        this.testParameters = testParameters

        setup(selectors)
        launchCustom()

        selectors.paymentSelection.click()

        FieldPopulator(
            selectors,
            testParameters,
            populateCustomLpmFields
        ).populateFields()

        Espresso.onIdle()
        composeTestRule.waitForIdle()
        val populatedFieldsScreenshot = getScreenshotBytes()

        // press continue -- this is failing, can't find the id, and not seeing it in the ui dump
        EspressoIdButton(R.id.continue_button).apply {
            scrollTo()
            click()
        }

        // press payment method
        Log.e("MLB", "paymentSheetFinishedLock.acquire")
        TimeUnit.SECONDS.sleep(1) // TODO: add another idling resource to wait for this
        Espresso.onIdle()
        composeTestRule.waitForIdle()
        EspressoIdButton(R.id.payment_method).click()

        TimeUnit.SECONDS.sleep(1)
        Espresso.onIdle()
        composeTestRule.waitForIdle()
        assertWithMessage("Screenshots differ").that(getScreenshotBytes())
            .isEqualTo(populatedFieldsScreenshot)
    }

    private fun pressAdd() {
        selectors.addButton.performClick()
        Espresso.onIdle()
        composeTestRule.waitForIdle()
    }

    private fun pressBack() {
        Espresso.pressBack()
        Espresso.onIdle()
        composeTestRule.waitForIdle()
    }

    private fun setup(selectors: Selectors) {
        registerListeners()
        setConfiguration(selectors)
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
        this.selectors = Selectors(device, composeTestRule, testParameters)
        this.testParameters = testParameters

        setup(selectors)
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

        selectors.buyButton.apply {
            scrollTo()
            click()
        }

        doAuthorization()
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

    internal fun registerListeners() {
        val launchPlayground = Semaphore(1)
        launchPlayground.acquire()
        // Setup the playground for scenario, and launch it.  We use the playground
        // so we don't have to implement another route to create a payment intent,
        // the challenge is that we don't have access to the activity or it's viewmodels
        val scenario = ActivityScenario.launch(PaymentSheetPlaygroundActivity::class.java)
        scenario.onActivity { activity ->

            IdlingPolicies.setIdlingResourceTimeout(45, TimeUnit.SECONDS)
            IdlingPolicies.setMasterPolicyTimeout(45, TimeUnit.SECONDS)

            IdlingRegistry.getInstance().register(
                activity.getMultiStepReadyIdlingResource(),
                activity.getSingleStepReadyIdlingResource(),
                activity.getMultiStepConfirmReadyIdlingResource(),
                TransitionFragmentResource.getSingleStepIdlingResource()
            )

            // Observe the result of the PaymentSheet completion
            activity.viewModel.status.observeForever {
                resultValue = it
                Log.e("MLB", "paymentSheetFinishedLock.release")
                paymentSheetFinishedLock.release()

                IdlingRegistry.getInstance().unregister(
                    activity.getMultiStepReadyIdlingResource(),
                    activity.getSingleStepReadyIdlingResource(),
                    activity.getMultiStepConfirmReadyIdlingResource(),
                    TransitionFragmentResource.getSingleStepIdlingResource()
                )
            }
            launchPlayground.release()
        }

        launchPlayground.acquire()
        launchPlayground.release()
    }

    internal fun setConfiguration(selectors: Selectors) {
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
        EspressoLabelIdButton(R.string.reload_paymentsheet).click()
        EspressoLabelIdButton(R.string.checkout_complete).click()

        // PaymentSheetActivity is now on screen
        Log.e("MLB", "paymentSheetFinishedLock.acquire")
        paymentSheetFinishedLock.acquire()
    }

    private fun launchCustom() {
        EspressoLabelIdButton(R.string.reload_paymentsheet).click()
        EspressoIdButton(R.id.payment_method).click()

        // PaymentSheetActivity is now on screen
        Log.e("MLB", "paymentSheetFinishedLock.acquire")

        paymentSheetFinishedLock.acquire()
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
            Log.e("MLB", "paymentSheetFinishedLock.acquire")

            paymentSheetFinishedLock.acquire()
            assertThat(resultValue).isEqualTo(
                PaymentSheetResult.Completed.toString()
            )
            Log.e("MLB", "paymentSheetFinishedLock.release")

            paymentSheetFinishedLock.release()
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
}
