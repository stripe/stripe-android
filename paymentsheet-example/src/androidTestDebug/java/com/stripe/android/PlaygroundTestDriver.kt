package com.stripe.android

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.screenshot.BasicScreenCaptureProcessor
import androidx.test.runner.screenshot.Screenshot
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.playground.activity.PaymentSheetPlaygroundActivity
import com.stripe.android.paymentsheet.viewmodels.transitionFragmentResource
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.AuBankAccountNumberSpec
import com.stripe.android.ui.core.elements.BankDropdownSpec
import com.stripe.android.ui.core.elements.BsbSpec
import com.stripe.android.ui.core.elements.CountrySpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IbanSpec
import com.stripe.android.ui.core.elements.KlarnaCountrySpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.SaveForFutureUseSpec
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.SimpleTextSpec
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

        // Setup the playground for scenario, and launch it
        launchCompleteScenario(testParameters) { activity ->
            callbackLock.acquire()

            // Observe the result of the action
            activity.viewModel.status.observeForever {
                resultValue = it
                callbackLock.release()
            }
        }

        selectPaymentMethod(composeTestRule, testParameters.paymentSelection)

        populateFields(composeTestRule, testParameters, populateCustomLpmFields)

        Espresso.closeSoftKeyboard()

        assert(testParameters.saveForFutureUseCheckboxVisible == SaveForFutureCheckbox.exists())
        if (SaveForFutureCheckbox.exists()) {
            if (!testParameters.saveCheckboxValue) {
                SaveForFutureCheckbox.click()
            }
        }

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

        IdlingRegistry.getInstance()
            .unregister(PaymentSheetPlaygroundActivity.singleStepUIIdlingResource)
        IdlingRegistry.getInstance()
            .unregister(PaymentSheetPlaygroundActivity.multiStepUIIdlingResource)
        IdlingRegistry.getInstance().unregister(transitionFragmentResource)

        callbackLock.acquire()
        Truth.assertThat(resultValue).isEqualTo(PaymentSheetResult.Completed.toString())
        callbackLock.release()
    }

    private fun selectPaymentMethod(
        composeTestRule: ComposeTestRule,
        paymentSelection: PaymentSelection
    ) {
        assert(paymentSelection.exists())
        paymentSelection.click(
            composeTestRule,
            InstrumentationRegistry.getInstrumentation().targetContext.resources
        )
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

        IdlingRegistry.getInstance()
            .register(PaymentSheetPlaygroundActivity.singleStepUIIdlingResource)
        IdlingRegistry.getInstance()
            .register(PaymentSheetPlaygroundActivity.multiStepUIIdlingResource)
        IdlingRegistry.getInstance().register(transitionFragmentResource)

        EspressoLabelIdButton(R.string.reload_paymentsheet).click()
        EspressoLabelIdButton(R.string.checkout_complete).click()
    }

    private fun populateFields(
        composeTestRule: ComposeTestRule,
        testParameters: TestParameters,
        populateCustomLpmFields: () -> Unit
    ) {
        populatePlatformLpmFields(
            composeTestRule,
            testParameters.paymentMethod.formSpec,
            testParameters.saveCheckboxValue,
            testParameters.billing
        )

        populateCustomLpmFields()
    }

    private fun populatePlatformLpmFields(
        composeTestRule: ComposeTestRule,
        formSpec: LayoutSpec,
        saveCheckboxValue: Boolean,
        defaultBillingOn: Billing
    ) {
        formSpec.items.forEach {
            when (it) {
                is SectionSpec -> {
                    if (!expectFieldToBeHidden(formSpec, saveCheckboxValue, it)) {
                        it.fields.forEach { sectionField ->
                            when (sectionField) {
                                is EmailSpec -> {
                                    if (defaultBillingOn == Billing.Off) {
                                        composeTestRule.onNodeWithText("Email")
                                            .performTextInput("jrosen@email.com")
                                    }
                                }
                                SimpleTextSpec.NAME -> {
                                    if (defaultBillingOn == Billing.Off) {
                                        composeTestRule.onNodeWithText("Name")
                                            .performTextInput("Jenny Rosen")
                                    }
                                }
                                is AddressSpec -> {
                                    if (defaultBillingOn == Billing.Off) {
                                        // TODO: This will not work when other countries are selected or defaulted
                                        composeTestRule.onNodeWithText("Address line 1")
                                            .performTextInput("123 Main Street")
                                        composeTestRule.onNodeWithText("City")
                                            .performTextInput("123 Main Street")
                                        composeTestRule.onNodeWithText("ZIP Code")
                                            .performTextInput("12345")
                                        composeTestRule.onNodeWithText("State")
                                            .performTextInput("NY")
                                    }
                                }
                                is CountrySpec -> {}
                                is SimpleTextSpec -> {}
                                AuBankAccountNumberSpec -> {}
                                is BankDropdownSpec -> {}
                                BsbSpec -> {}
                                IbanSpec -> {}
                                is KlarnaCountrySpec -> {}
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }


    private fun expectFieldToBeHidden(
        formSpec: LayoutSpec,
        saveCheckboxValue: Boolean,
        section: SectionSpec
    ): Boolean {
        val saveForFutureUseSpec = formSpec.items
            .mapNotNull { it as? SaveForFutureUseSpec }
            .firstOrNull()
        return (!saveCheckboxValue
            && saveForFutureUseSpec?.identifierRequiredForFutureUse
            ?.map { saveForFutureUseHidesIdentifier ->
                saveForFutureUseHidesIdentifier.identifier.value
            }
            ?.firstOrNull { saveForFutureUseHidesIdentifier ->
                saveForFutureUseHidesIdentifier == section.identifier.value
            } != null)

    }
}
