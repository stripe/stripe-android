package com.stripe.android

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
        // the challeng is that we don't have access to the activity or it's viewmodels
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

    private fun doAuthorization(
        device: UiDevice,
        authorizationAction: AuthorizeAction,
        useBrowser: Browser?
    ) {
        // Which browsers are available on device - chrome and or firefox
        val browser = Browser.Chrome
        if (SelectBrowserWindow.exists(device)) {
            // Prefer the browser selected in the parameters
            useBrowser?.click(device)
        }

        if (AuthorizeWindow.exists(device, browser)) {
            AuthorizePageLoaded.blockUntilLoaded(device)
            when (authorizationAction) {
                AuthorizeAction.Authorize, AuthorizeAction.Fail ->
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
        testParameters.googlePayState.click()
        testParameters.currency.click()
        testParameters.checkout.click()
        testParameters.billing.click()
        testParameters.delayed.click()

        EspressoLabelIdButton(R.string.reload_paymentsheet).click()
        EspressoLabelIdButton(R.string.checkout_complete).click()
    }
}
