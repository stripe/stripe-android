package com.stripe.android

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.screenshot.Screenshot
import androidx.test.uiautomator.UiDevice
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.result.Result
import com.google.common.truth.Truth
import com.google.gson.Gson
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.playground.activity.PaymentSheetPlaygroundActivity
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.CheckoutResponse
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.paymentsheet.viewmodels.transitionFragmentResource
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.SectionSpec
import com.stripe.android.ui.core.elements.SimpleTextSpec
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * This one doesn't care if it is a new or returning customer
 */
internal fun confirmCompleteSuccess(
    device: UiDevice,
    composeTestRule: ComposeTestRule,
    basicScreenCaptureProcessor: MyScreenCaptureProcessor,
    testParameters: TestParameters,
    populateCustomLpmFields: () -> Unit = {}
) {
    // TODO: I can imagine a different test that checks the different authorization states.

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

    assert(testParameters.paymentSelection.exists())
    testParameters.paymentSelection.click(InstrumentationRegistry.getInstrumentation().targetContext.resources)

    populatePlatformLpmFields(composeTestRule, testParameters)

    populateCustomLpmFields()

    Espresso.closeSoftKeyboard()

    assert(testParameters.saveForFutureUseCheckboxVisible == SaveForFutureCheckbox.exists())
    if (SaveForFutureCheckbox.exists()) {
        if (!testParameters.saveCheckboxValue) {
            SaveForFutureCheckbox.click()
        }
    }

    val capture = Screenshot.capture()
    capture.name =
        "${testParameters.checkout.javaClass.name}.${getResourceString(testParameters.paymentSelection.label)}"
    capture.process(setOf(basicScreenCaptureProcessor))

    Espresso.onView(ViewMatchers.withId(R.id.buy_button))
        .perform(ViewActions.scrollTo())
    Espresso.onView(ViewMatchers.withId(R.id.buy_button)).perform(ViewActions.click())

    TimeUnit.SECONDS.sleep(1) // TODO: Need to rid of this before the browser selector comes up
    val expectedAuthorizationParameters = testParameters.authorizationParameters
    if (expectedAuthorizationParameters != null) {
        if (SelectBrowserWindow.exists(device)) {
            expectedAuthorizationParameters.browser.click(device)
        }

        if (AuthorizeWindow.exists(device, expectedAuthorizationParameters.browser)) {
            AuthorizePageLoaded.blockUntilLoaded(device)
            when (expectedAuthorizationParameters.action) {
                AuthorizeAction.Authorize, AuthorizeAction.Fail ->
                    expectedAuthorizationParameters.action.click(device)
                AuthorizeAction.Cancel ->
                    device.pressBack()
            }
        }
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

    IdlingRegistry.getInstance().register(PaymentSheetPlaygroundActivity.singleStepUIIdlingResource)
    IdlingRegistry.getInstance().register(PaymentSheetPlaygroundActivity.multiStepUIIdlingResource)
    IdlingRegistry.getInstance().register(transitionFragmentResource)

    EspressoLabelIdButton(R.string.reload_paymentsheet).click()
    Espresso.onView(ViewMatchers.withText(R.string.checkout_complete)).perform(ViewActions.click())
}

private fun populatePlatformLpmFields(
    composeTestRule: ComposeTestRule,
    testParameters: TestParameters
) {
    // Also remove the default billing address
    testParameters.paymentMethod.formSpec.items.forEach {
        when (it) {
            is SectionSpec -> {
                it.fields.forEach { sectionField ->
                    when (sectionField) {
                        is EmailSpec -> {
                            composeTestRule.onNodeWithText("Email")
                                .performTextInput("jrosen@email.com")
                        }
                        SimpleTextSpec.NAME -> {
                            composeTestRule.onNodeWithText("Name")
                                .performTextInput("Jenny Rosen")

                        }
                        is AddressSpec -> {
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
//                        is BankDropdownSpec -> {}
//                        is CountrySpec -> {}
//                        is KlarnaCountrySpec -> {}
//                        is SimpleTextSpec -> {}
                    }
                }
            }
            else -> {}
        }
    }

}

internal suspend fun checkoutSessionSetup(testParameters: TestParameters): CheckoutResponse? {
    val requestBody = CheckoutRequest(
        testParameters.customer.toRepository().value,
        testParameters.currency.toRepository().value,
        testParameters.checkout.toRepository().value,
        set_shipping_address = false,
        automatic_payment_methods = false
    )

    return suspendCoroutine { continuation ->
        Fuel.post("https://innate-lofty-napkin.glitch.me/checkout")
            .jsonBody(Gson().toJson(requestBody))
            .responseString { _, _, result ->
                when (result) {
                    is Result.Failure -> {
                        continuation.resume(null)
                    }
                    is Result.Success -> {
                        val checkoutResponse =
                            Gson().fromJson(result.get(), CheckoutResponse::class.java)

                        // Init PaymentConfiguration with the publishable key returned from the backend,
                        // which will be used on all Stripe API calls
                        PaymentConfiguration.init(
                            InstrumentationRegistry.getInstrumentation().targetContext,
                            checkoutResponse.publishableKey
                        )

                        continuation.resume(checkoutResponse)
                    }
                }
            }
    }
}

data class TestParameters(
    val paymentMethod: SupportedPaymentMethod,
    val customer: Customer,
    val googlePayState: GooglePayState,
    val currency: Currency,
    val checkout: Checkout,
    val billing: Billing,
    val delayed: DelayedPMs,
    val automatic: Automatic,
    val saveCheckboxValue: Boolean,
    val saveForFutureUseCheckboxVisible: Boolean,
    val authorizationParameters: AuthorizationParameters?
) {
    val paymentSelection = PaymentSelection(paymentMethod.displayNameResource)
}

data class AuthorizationParameters(
    val browser: Browser,
    val action: AuthorizeAction
)
