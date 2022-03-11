package com.stripe.android

import android.content.pm.PackageManager
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.IdlingRegistry
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.screenshot.BasicScreenCaptureProcessor
import androidx.test.runner.screenshot.Screenshot
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.playground.activity.PaymentSheetPlaygroundActivity
import com.stripe.android.paymentsheet.viewmodels.TransitionFragmentResource
import org.junit.Assume.assumeTrue
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit


class PlaygroundTestDriver(
    private val device: UiDevice,
    private val composeTestRule: ComposeTestRule,
    private val basicScreenCaptureProcessor: MyScreenCaptureProcessor
) {
    fun confirmNewOrGuestCompleteSuccess(
        testParameters: TestParameters,
        populateCustomLpmFields: () -> Unit = {}
    ) {
        var resultValue: String? = null
        val callbackLock = Semaphore(1)

        // Setup the playground for scenario, and launch it.  We use the playground
        // so we don't have to implement another route to create a payment intent,
        // the challenge is that we don't have access to the activity or it's viewmodels
        launchCompleteScenario(testParameters) { activity ->
            callbackLock.acquire()

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

        // PaymentSheetActivity is now on screen

        testParameters.paymentSelection.click(
            composeTestRule,
            InstrumentationRegistry.getInstrumentation().targetContext.resources
        )

        FieldPopulator(composeTestRule, testParameters, populateCustomLpmFields).populateFields()

        takeScreenShot(
            basicScreenCaptureProcessor,
            "${testParameters.checkout.javaClass.name}." +
                getResourceString(testParameters.paymentSelection.label)
        )

        // Verify device requirements are met prior to attempting confirmation.  Do this
        // after we have had the chance to capture a screenshot.
        testParameters.authorizationAction?.let {
            testParameters.useBrowser?.let {
                assumeTrue(getInstalledBrowser(testParameters.useBrowser) == it)
            } ?: assumeTrue(getInstalledBrowsers().isNotEmpty())
        }

        clickBuyButton()

        // TODO: Need to rid of this sleep before the browser selector comes up
        TimeUnit.SECONDS.sleep(1)
        if (testParameters.authorizationAction != null) {
            doAuthorization(
                device,
                testParameters.authorizationAction,
                testParameters.useBrowser
            )
        } else {
            assert(!SelectBrowserWindow.exists(device))
        }

        callbackLock.acquire()
        assertThat(resultValue).isEqualTo(PaymentSheetResult.Completed.toString())
        callbackLock.release()
    }

    private fun getInstalledPackages() = InstrumentationRegistry
        .getInstrumentation()
        .targetContext
        .packageManager
        .getInstalledApplications(PackageManager.GET_META_DATA)

    private fun getInstalledBrowser(requestedBrowser: Browser?): Browser {
        val installedBrowsers = getInstalledBrowsers()

        return requestedBrowser?.let {
            // Assume true will mark the test as skipped if it can't be executed
            assumeTrue(installedBrowsers.contains(it))
            it
        } ?: installedBrowsers.first()
    }

    private fun getInstalledBrowsers() = getInstalledPackages()
        .mapNotNull { Browser.to(it.packageName) }

    private fun doAuthorization(
        device: UiDevice,
        authorizationAction: AuthorizeAction,
        useBrowser: Browser?
    ) {
        val selectedBrowser = getInstalledBrowser(useBrowser)

        if (SelectBrowserWindow.exists(device)) {
            selectedBrowser.click(device)
        }

        if (AuthorizeWindow.exists(device, selectedBrowser)) {
            AuthorizePageLoaded.blockUntilLoaded(device)
            when (authorizationAction) {
                is AuthorizeAction.Authorize,
                is AuthorizeAction.Fail ->
                    authorizationAction.click(device)
                AuthorizeAction.Cancel ->
                    device.pressBack()
            }
        }
    }

    private fun clickBuyButton() {
        PlaygroundBuyButton.scrollTo()
        PlaygroundBuyButton.click()
    }

    private fun takeScreenShot(
        basicScreenCaptureProcessor: BasicScreenCaptureProcessor,
        fileName: String
    ) {
        val capture = Screenshot.capture()
        capture.name = fileName

        capture.process(setOf(basicScreenCaptureProcessor))
    }

    private fun getResourceString(id: Int) =
        InstrumentationRegistry.getInstrumentation().targetContext.resources.getString(id)

    private fun launchCompleteScenario(
        testParameters: TestParameters,
        onActivity: (PaymentSheetPlaygroundActivity) -> Unit
    ) {
        val scenario = ActivityScenario.launch(PaymentSheetPlaygroundActivity::class.java)
        scenario.onActivity { activity ->
            onActivity(activity as PaymentSheetPlaygroundActivity)
        }

        testParameters.customer.click()
        testParameters.currency.click()
        testParameters.checkout.click()
        testParameters.billing.click()
        testParameters.delayed.click()

        // Can't guarantee that google pay will be on the phone
        testParameters.googlePayState.click()

        EspressoLabelIdButton(R.string.reload_paymentsheet).click()
        EspressoLabelIdButton(R.string.checkout_complete).click()
    }
}
