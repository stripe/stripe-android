package com.stripe.android.test.core

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.runner.screenshot.Screenshot
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.playground.activity.PaymentSheetPlaygroundActivity
import com.stripe.android.paymentsheet.viewmodels.TransitionFragmentResource
import com.stripe.android.test.core.ui.BrowserUI
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
    private val callbackLock = Semaphore(1)
    private lateinit var testParameters: TestParameters
    private lateinit var selectors: Selectors

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

        registerListeners()
        launchComplete(selectors)

        // PaymentSheetActivity is now on screen
        callbackLock.acquire()

        selectors.paymentSelection.click()

        takeScreenShot(
            fileName = "${selectors.baseScreenshotFilenamePrefix}-beforeText",
            testParameters.takeScreenshotOnLpmLoad
        )

        FieldPopulator(
            selectors,
            testParameters,
            populateCustomLpmFields
        ).populateFields()

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

        callbackLock.release()
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
                activity.getMultiStepIdlingResource(),
                activity.getSingleStepIdlingResource(),
                TransitionFragmentResource.getSingleStepIdlingResource()
            )

            // Observe the result of the action
            activity.viewModel.status.observeForever {
                resultValue = it
                callbackLock.release()

                IdlingRegistry.getInstance().unregister(
                    activity.getMultiStepIdlingResource(),
                    activity.getSingleStepIdlingResource(),
                    TransitionFragmentResource.getSingleStepIdlingResource()
                )
            }
            launchPlayground.release()
        }

        launchPlayground.acquire()
        launchPlayground.release()
    }

    internal fun launchComplete(selectors: Selectors) {
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

        EspressoLabelIdButton(R.string.reload_paymentsheet).click()
        EspressoLabelIdButton(R.string.checkout_complete).click()
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
            callbackLock.acquire()
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
}
