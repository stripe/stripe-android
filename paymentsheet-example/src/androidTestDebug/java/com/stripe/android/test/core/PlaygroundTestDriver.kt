package com.stripe.android.test.core

import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.IdlingRegistry
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.screenshot.Screenshot
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.playground.activity.PaymentSheetPlaygroundActivity
import com.stripe.android.paymentsheet.viewmodels.TransitionFragmentResource
import org.junit.Assume
import java.util.concurrent.Semaphore

class PlaygroundTestDriver(
    private val device: UiDevice,
    private val composeTestRule: ComposeTestRule,
    private val basicScreenCaptureProcessor: MyScreenCaptureProcessor,
) {

    // TODO: Playground looks a little funny with the buttons we can scroll to the buy button now
    // TODO: Test with setup intents as well.
    // TODO: Need a card test when compose is in.
    // TODO: Dropdown
    // TODO: Localize address fields

    private var resultValue: String? = null
    private val callbackLock = Semaphore(1)

    fun confirmNewOrGuestComplete(
        testParameters: TestParameters,
        populateCustomLpmFields: () -> Unit = {}
    ) {
        val baseScreenshotFilenamePrefix = "info-" +
            getResourceString(testParameters.paymentSelection.label) +
            "-" +
            testParameters.checkout.javaClass.name

        registerListeners()
        launchComplete(testParameters)

        // PaymentSheetActivity is now on screen
        callbackLock.acquire()

        testParameters.paymentSelection.click(
            composeTestRule,
            InstrumentationRegistry.getInstrumentation().targetContext.resources
        )

        takeScreenShot(
            fileName = "$baseScreenshotFilenamePrefix-beforeText",
            testParameters.takeScreenshotOnLpmLoad
        )

        FieldPopulator(
            composeTestRule,
            testParameters,
            populateCustomLpmFields
        ).populateFields()

        takeScreenShot(
            fileName = "$baseScreenshotFilenamePrefix-afterText",
            testParameters.takeScreenshotOnLpmLoad
        )

        // Verify device requirements are met prior to attempting confirmation.  Do this
        // after we have had the chance to capture a screenshot.
        verifyDeviceSupportsTestAuthorization(testParameters)

        clickBuyButton()

        doAuthorization(testParameters.authorizationAction, testParameters.useBrowser)

        callbackLock.release()
    }

    private fun verifyDeviceSupportsTestAuthorization(testParameters: TestParameters) {
        testParameters.authorizationAction?.let {
            testParameters.useBrowser?.let {
                Assume.assumeTrue(getInstalledBrowser(testParameters.useBrowser) == it)
            } ?: Assume.assumeTrue(getInstalledBrowsers().isNotEmpty())
        }
    }

    private fun registerListeners() {
        val launchPlayground = Semaphore(1)
        launchPlayground.acquire()
        // Setup the playground for scenario, and launch it.  We use the playground
        // so we don't have to implement another route to create a payment intent,
        // the challenge is that we don't have access to the activity or it's viewmodels
        val scenario = ActivityScenario.launch(PaymentSheetPlaygroundActivity::class.java)
        scenario.onActivity { activity ->
            launchPlayground.release()
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
        }
        launchPlayground.acquire()
        launchPlayground.release()

    }

    private fun launchComplete(testParameters: TestParameters) {
        testParameters.customer.click()
        testParameters.currency.click()
        testParameters.checkout.click()
        testParameters.billing.click()
        testParameters.delayed.click()

        // Can't guarantee that google pay will be on the phone
        testParameters.googlePayState.click()

        LabelIdButton(R.string.reload_paymentsheet).click()
        LabelIdButton(R.string.checkout_complete).click()
    }

    private fun getInstalledPackages() = InstrumentationRegistry.getInstrumentation()
        .targetContext
        .packageManager
        .getInstalledApplications(PackageManager.GET_META_DATA)

    private fun getInstalledBrowser(requestedBrowser: Browser?): Browser {
        val installedBrowsers = getInstalledBrowsers()

        return requestedBrowser?.let {
            // Assume true will mark the test as skipped if it can't be executed
            Assume.assumeTrue(installedBrowsers.contains(it))
            it
        } ?: installedBrowsers.first()
    }

    private fun getInstalledBrowsers() = getInstalledPackages()
        .mapNotNull { Browser.to(it.packageName) }

    private fun doAuthorization(
        authorizationAction: AuthorizeAction?,
        useBrowser: Browser?
    ) {
        if (authorizationAction != null) {
            val selectedBrowser = getInstalledBrowser(useBrowser)

            SelectBrowserWindow.wait(device, 2000)
            if (SelectBrowserWindow.exists(device)) {
                selectedBrowser.click(device)
            }
            if (AuthorizeWindow.exists(device, selectedBrowser)) {
                AuthorizePageLoaded.blockUntilLoaded(device)
                authorizationAction.click(device)
                when (authorizationAction) {
                    AuthorizeAction.Authorize -> {}
                    AuthorizeAction.Cancel -> {
                        PlaygroundBuyButton.apply {
                            waitProcessingComplete(device)
                            isEnabled()
                            isDisplayed()
                        }
                    }
                    AuthorizeAction.Fail -> {
                        PlaygroundBuyButton.apply {
                            waitProcessingComplete(device)
                            isEnabled()
                            isDisplayed()
                        }

                        // The text comes after the buy button animation is complete
                        EspressoText(
                            "We are unable to authenticate your payment method. Please " +
                                "choose a different payment method and try again."
                        ).isDisplayed()
                    }
                }
            }
        } else {
            assert(!SelectBrowserWindow.exists(device))
        }


        if (authorizationAction == AuthorizeAction.Authorize || authorizationAction == null) {
            callbackLock.acquire()
            assertThat(resultValue).isEqualTo(
                PaymentSheetResult.Completed.toString()
            )
        }
    }

    private fun clickBuyButton() {
        PlaygroundBuyButton.apply {
            scrollTo()
            click()
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

    private fun getResourceString(id: Int) =
        InstrumentationRegistry.getInstrumentation().targetContext.resources.getString(id)
}
