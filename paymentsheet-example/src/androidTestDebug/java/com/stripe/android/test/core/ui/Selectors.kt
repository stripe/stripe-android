package com.stripe.android.test.core.ui

import android.content.pm.PackageManager
import androidx.annotation.StringRes
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.WalletConstants
import com.google.common.truth.Truth.assertThat
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.Automatic
import com.stripe.android.test.core.Billing
import com.stripe.android.test.core.Currency
import com.stripe.android.test.core.Customer
import com.stripe.android.test.core.DelayedPMs
import com.stripe.android.test.core.GooglePayState
import com.stripe.android.test.core.HOOKS_PAGE_LOAD_TIMEOUT
import com.stripe.android.test.core.IntentType
import com.stripe.android.test.core.Shipping
import com.stripe.android.test.core.TestParameters
import com.stripe.android.ui.core.elements.SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG

/**
 * This contains the Android specific code such as for accessing UI elements, detecting
 * the installed browsers.  It also abstracts away the testing platform, espresso vs. compose.
 */
class Selectors(
    val device: UiDevice,
    val composeTestRule: ComposeTestRule,
    testParameters: TestParameters
) {
    val testMode = EspressoIdButton(R.id.testmode)
    val continueButton = EspressoIdButton(R.id.continue_button)
    val complete = EspressoLabelIdButton(R.string.checkout_complete)
    val reload = EspressoLabelIdButton(R.string.reload_paymentsheet)
    val multiStepSelect = EspressoIdButton(R.id.payment_method)
    val saveForFutureCheckbox = composeTestRule
        .onNodeWithTag(SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG)

    val customer = when (testParameters.customer) {
        Customer.Guest -> EspressoLabelIdButton(R.string.customer_guest)
        Customer.New -> EspressoLabelIdButton(R.string.customer_new)
        Customer.Returning -> EspressoLabelIdButton(R.string.customer_returning)
    }
    val googlePayState = when (testParameters.googlePayState) {
        GooglePayState.Off -> EspressoIdButton(R.id.google_pay_off_button)
        GooglePayState.On -> EspressoIdButton(R.id.google_pay_on_button)
    }
    val currency = when (testParameters.currency) {
        Currency.EUR -> EspressoLabelIdButton(R.string.currency_eur)
        Currency.USD -> EspressoLabelIdButton(R.string.currency_usd)
        Currency.AUD -> EspressoLabelIdButton(R.string.currency_aud)
        Currency.GBP -> EspressoLabelIdButton(R.string.currency_gbp)
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

    val editButton = EditButton(device)

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
        device.waitForIdle()
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


    fun onGooglePayAvailable(availableCallable: () -> Unit, unavailableCallable: () -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val googlePayJsonFactory = GooglePayJsonFactory(context)

        val paymentsClient: PaymentsClient by lazy {
            val options = com.google.android.gms.wallet.Wallet.WalletOptions.Builder()
                .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                .build()

            com.google.android.gms.wallet.Wallet.getPaymentsClient(context, options)
        }

        val request = IsReadyToPayRequest.fromJson(
            googlePayJsonFactory.createIsReadyToPayRequest(
                billingAddressParameters = GooglePayJsonFactory.BillingAddressParameters(
                    false,
                    GooglePayJsonFactory.BillingAddressParameters.Format.Min,
                    false
                ),
                existingPaymentMethodRequired = true
            ).toString()
        )

        paymentsClient.isReadyToPay(request)
            .addOnCompleteListener { task ->
                val isReady = runCatching {
                    task.getResult(ApiException::class.java) == true
                }.getOrDefault(false)
                if (isReady) {
                    availableCallable()
                } else {
                    unavailableCallable()
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
                className = "android.widget.TextView",
                device = device
            ) {}
        }
        else -> null
    }

    fun getResourceString(id: Int) =
        InstrumentationRegistry.getInstrumentation().targetContext.resources.getString(id)

    // Note: Compose will take care of scrolling to the field if not in view.
    fun getEmail() = composeTestRule.onNodeWithText(
        getResourceString(R.string.email)
    )

    fun getName(@StringRes resourceId: Int) = composeTestRule.onNodeWithText(
        getResourceString(resourceId)
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

    fun getAuBsb() = composeTestRule.onNodeWithText(
        getResourceString(com.stripe.android.ui.core.R.string.becs_widget_bsb)
    )

    fun getAuAccountNumber() = composeTestRule.onNodeWithText(
        getResourceString(R.string.becs_widget_account_number)
    )

    fun getGoogleDividerText() = composeTestRule.onNodeWithText(
        "Or pay",
        substring = true,
        useUnmergedTree = true
    )

    fun getCardNumber() = composeTestRule.onNodeWithText(
        InstrumentationRegistry.getInstrumentation().targetContext.resources.getString(
            com.stripe.android.R.string.acc_label_card_number
        )
    )

    fun getCardExpiration() = composeTestRule.onNodeWithText(
        InstrumentationRegistry.getInstrumentation().targetContext.resources.getString(
            R.string.stripe_paymentsheet_expiration_date_hint
        )
    )

    fun getCardCvc() = composeTestRule.onNodeWithText(
        InstrumentationRegistry.getInstrumentation().targetContext.resources.getString(
            com.stripe.android.ui.core.R.string.cvc_number_hint
        )
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
