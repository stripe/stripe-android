package com.stripe.android.test.core.ui

import android.content.pm.PackageManager
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.Automatic
import com.stripe.android.test.core.Billing
import com.stripe.android.test.core.IntentType
import com.stripe.android.test.core.Currency
import com.stripe.android.test.core.Customer
import com.stripe.android.test.core.DelayedPMs
import com.stripe.android.test.core.GooglePayState
import com.stripe.android.test.core.HOOKS_PAGE_LOAD_TIMEOUT
import com.stripe.android.test.core.Shipping
import com.stripe.android.test.core.TestParameters

/**
 * This contains the Android specific code such as for accessing UI elements, detecting
 * the installed browsers.  It also abstracts away the testing platform, espresso vs. compose.
 */
class Selectors(
    val device: UiDevice,
    val composeTestRule: ComposeTestRule,
    testParameters: TestParameters
) {
    val saveForFutureCheckbox =
        EspressoLabelIdButton(R.string.stripe_paymentsheet_save_this_card_with_merchant_name)
    val customer = when (testParameters.customer) {
        Customer.Guest -> EspressoLabelIdButton(R.string.customer_guest)
        Customer.New -> EspressoLabelIdButton(R.string.customer_new)
    }
    val googlePayState = when (testParameters.googlePayState) {
        GooglePayState.Off -> EspressoIdButton(R.id.google_pay_off_button)
        GooglePayState.On -> EspressoIdButton(R.id.google_pay_on_button)
    }
    val currency = when (testParameters.currency) {
        Currency.EUR -> EspressoLabelIdButton(R.string.currency_eur)
        Currency.USD -> EspressoLabelIdButton(R.string.currency_usd)
    }

    val checkout = when (testParameters.intentType) {
        IntentType.Pay -> EspressoLabelIdButton(R.string.payment)
        IntentType.PayWithSetup -> EspressoLabelIdButton(R.string.payment_with_setup)
        IntentType.Setup -> EspressoLabelIdButton(R.string.setup)
    }
    val billing = when (testParameters.billing) {
        Billing.Off -> EspressoIdButton(R.id.default_billing_off_button)
        Billing.On -> EspressoIdButton(R.id.default_billing_on_button)
    }

    val shipping = when (testParameters.shipping) {
        Shipping.Off -> EspressoIdButton(R.id.shipping_off_button)
        Shipping.On -> EspressoIdButton(R.id.shipping_on_button)
    }

    val delayed = when (testParameters.delayed) {
        DelayedPMs.Off -> EspressoIdButton(R.id.allowsDelayedPaymentMethods_off_button)
        DelayedPMs.On -> EspressoIdButton(R.id.allowsDelayedPaymentMethods_on_button)
    }

    val automatic = when (testParameters.automatic) {
        Automatic.Off -> EspressoIdButton(R.id.automatic_pm_off_button)
        Automatic.On -> EspressoIdButton(R.id.automatic_pm_on_button)
    }

    val paymentSelection = PaymentSelection(
        composeTestRule,
        testParameters.paymentMethod.displayNameResource
    )

    val baseScreenshotFilenamePrefix = "info-" +
        getResourceString(paymentSelection.label) +
        "-" +
        testParameters.intentType.name

    val buyButton = BuyButton(device)

    val selectBrowserPrompt = UiAutomatorText("Verify your payment", device = device)

    fun browserIconAtPrompt(browser: BrowserUI) = UiAutomatorText(browser.name, device = device)

    fun browserWindow(browser: BrowserUI): UiObject? = browserWindow(device, browser)

    fun blockUntilAuthorizationPageLoaded() {
        assertThat(
            device.wait(
                Until.findObject(
                    By.textContains("test payment page")
                ),
                HOOKS_PAGE_LOAD_TIMEOUT * 1000
            )
        ).isNotNull()
        device.waitForIdle()// TODO: Is this needed?
    }

    fun getInstalledBrowsers() = getInstalledPackages()
        .mapNotNull {
            when (it.packageName) {
                BrowserUI.Firefox.packageName -> BrowserUI.Firefox
                BrowserUI.Chrome.packageName -> BrowserUI.Chrome
                else -> {
                    null
                }
            }
        }

    private fun getInstalledPackages() = InstrumentationRegistry.getInstrumentation()
        .targetContext
        .packageManager
        .getInstalledApplications(PackageManager.GET_META_DATA)

    val authorizeAction = when (testParameters.authorizationAction) {
        AuthorizeAction.Authorize -> {
            object : UiAutomatorText(
                label = testParameters.authorizationAction.text,
                className = "android.widget.Button",
                device = device
            ) {
                override fun click() {
                    // Afterpay, giropay and some other authorization test pages
                    // have their authorization class name of TextView instead of button,
                    // in other cases such as bancontact and ideal the authorize class prevents
                    // it from finding the button.  More investigation into why is needed.
                    if (!exists()) {
                        className = null
                    }

                    super.click()
                }
            }
        }
        AuthorizeAction.Cancel -> {
            object : UiAutomatorText(
                label = testParameters.authorizationAction.text,
                className = "android.widget.Button",
                device = device
            ) {
                override fun click() {
                    device.pressBack()
                }
            }
        }
        AuthorizeAction.Fail -> {
            object : UiAutomatorText(
                label = testParameters.authorizationAction.text,
                className = "android.widget.Button",
                device = device
            ) {}
        }
        else -> null
    }

    private fun getResourceString(id: Int) =
        InstrumentationRegistry.getInstrumentation().targetContext.resources.getString(id)

    // Note: Compose will take care of scrolling to the field if not in view.
    fun getEmail() = composeTestRule.onNodeWithText(
        getResourceString(R.string.email)
    )

    fun getName() = composeTestRule.onNodeWithText(
        getResourceString(R.string.address_label_name)
    )

    fun getLine1() = composeTestRule.onNodeWithText(
        getResourceString(R.string.address_label_address_line1)
    )

    fun getCity() = composeTestRule.onNodeWithText(
        getResourceString(R.string.address_label_city)
    )

    fun getState() = composeTestRule.onNodeWithText(
        getResourceString(R.string.address_label_state)
    )

    fun getZip() = composeTestRule.onNodeWithText(
        getResourceString(R.string.address_label_zip_code)
    )

    companion object {
        fun browserWindow(device: UiDevice, browser: BrowserUI): UiObject? =
            device.findObject(
                UiSelector()
                    .packageName(browser.packageName)
                    .resourceId(browser.resourceID)
            )
    }
}
