package com.stripe.android.test.core.ui

import android.content.pm.PackageManager
import androidx.annotation.StringRes
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.playground.model.InitializationType
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.Automatic
import com.stripe.android.test.core.Billing
import com.stripe.android.test.core.Currency
import com.stripe.android.test.core.Customer
import com.stripe.android.test.core.DelayedPMs
import com.stripe.android.test.core.GooglePayState
import com.stripe.android.test.core.HOOKS_PAGE_LOAD_TIMEOUT
import com.stripe.android.test.core.IntentType
import com.stripe.android.test.core.LinkState
import com.stripe.android.test.core.Shipping
import com.stripe.android.test.core.TestParameters
import com.stripe.android.ui.core.elements.SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG
import java.util.Locale
import com.stripe.android.R as StripeR
import com.stripe.android.core.R as CoreR
import com.stripe.android.uicore.R as UiCoreR

/**
 * This contains the Android specific code such as for accessing UI elements, detecting
 * the installed browsers.  It also abstracts away the testing platform, espresso vs. compose.
 */
class Selectors(
    val device: UiDevice,
    val composeTestRule: ComposeTestRule,
    testParameters: TestParameters
) {
    val reset = EspressoIdButton(R.id.reset_button)
    val continueButton = BuyButton(composeTestRule)
    val complete = EspressoIdButton(R.id.complete_checkout_button)
    val reload = EspressoLabelIdButton(R.string.reload_paymentsheet)
    val multiStepSelect = EspressoIdButton(R.id.payment_method)
    val saveForFutureCheckbox = composeTestRule
        .onNodeWithTag(SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG)

    val customer = when (testParameters.customer) {
        Customer.Guest -> EspressoLabelIdButton(R.string.customer_guest)
        Customer.New -> EspressoLabelIdButton(R.string.customer_new)
        Customer.Returning -> EspressoLabelIdButton(R.string.customer_returning)
    }

    val linkState = when (testParameters.linkState) {
        LinkState.Off -> EspressoIdButton(R.id.link_off_button)
        LinkState.On -> EspressoIdButton(R.id.link_on_button)
    }

    val googlePayState = when (testParameters.googlePayState) {
        GooglePayState.Off -> EspressoIdButton(R.id.google_pay_off_button)
        GooglePayState.On -> EspressoIdButton(R.id.google_pay_on_button)
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
        Shipping.OnWithDefaults -> EspressoIdButton(R.id.shipping_on_with_defaults_button)
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

    val attachDefaults = if (testParameters.attachDefaults) {
        EspressoIdButton(R.id.attach_defaults_on_button)
    } else {
        EspressoIdButton(R.id.attach_defaults_off_button)
    }

    val collectName = when (testParameters.collectName) {
        PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic -> EspressoIdButton(R.id.collect_name_radio_auto)
        PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always -> EspressoIdButton(R.id.collect_name_radio_always)
        PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never -> EspressoIdButton(R.id.collect_name_radio_never)
    }

    val collectEmail = when (testParameters.collectEmail) {
        PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic -> EspressoIdButton(R.id.collect_email_radio_auto)
        PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always -> EspressoIdButton(R.id.collect_email_radio_always)
        PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never -> EspressoIdButton(R.id.collect_email_radio_never)
    }

    val collectPhone = when (testParameters.collectPhone) {
        PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Automatic -> EspressoIdButton(R.id.collect_phone_radio_auto)
        PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always -> EspressoIdButton(R.id.collect_phone_radio_always)
        PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never -> EspressoIdButton(R.id.collect_phone_radio_never)
    }

    val collectAddress = when (testParameters.collectAddress) {
        PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Automatic -> EspressoIdButton(R.id.collect_address_radio_auto)
        PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full -> EspressoIdButton(R.id.collect_address_radio_full)
        PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never -> EspressoIdButton(R.id.collect_address_radio_never)
    }

    val baseScreenshotFilenamePrefix = "info-" +
        getResourceString(paymentSelection.label) +
        "-" +
        testParameters.intentType.name

    val buyButton = BuyButton(composeTestRule)

    val addPaymentMethodButton = AddPaymentMethodButton(device)

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

    private fun getInstalledPackages() = InstrumentationRegistry.getInstrumentation()
        .targetContext
        .packageManager
        .getInstalledApplications(PackageManager.GET_META_DATA)

    val authorizeAction = when (testParameters.authorizationAction) {
        is AuthorizeAction.Authorize -> {
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
        is AuthorizeAction.Cancel -> {
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
        is AuthorizeAction.Fail -> {
            object : UiAutomatorText(
                label = testParameters.authorizationAction.text,
                className = "android.widget.Button",
                device = device
            ) {}
        }
        else -> null
    }

    fun getResourceString(id: Int) =
        InstrumentationRegistry.getInstrumentation().targetContext.resources.getString(id)

    // Note: Compose will take care of scrolling to the field if not in view.
    fun getEmail() = composeTestRule.onNodeWithText(
        getResourceString(UiCoreR.string.stripe_email)
    )

    fun getName(@StringRes resourceId: Int) = composeTestRule.onNodeWithText(
        getResourceString(resourceId)
    )

    fun getLine1() = composeTestRule.onNodeWithText(
        getResourceString(CoreR.string.stripe_address_label_address_line1)
    )

    fun getCity() = composeTestRule.onNodeWithText(
        getResourceString(CoreR.string.stripe_address_label_city)
    )

    fun getState() = composeTestRule.onNodeWithText(
        getResourceString(CoreR.string.stripe_address_label_state)
    )

    fun selectState(value: String) {
        composeTestRule.onNodeWithText(getResourceString(CoreR.string.stripe_address_label_state))
            .performClick()
        composeTestRule.onNodeWithText(value)
            .performClick()
    }

    fun getZip() = composeTestRule.onNodeWithText(
        getResourceString(CoreR.string.stripe_address_label_zip_code)
    )

    fun getAuBsb() = composeTestRule.onNodeWithText(
        getResourceString(StripeR.string.stripe_becs_widget_bsb)
    )

    fun getAuAccountNumber() = composeTestRule.onNodeWithText(
        getResourceString(StripeR.string.stripe_becs_widget_account_number)
    )

    fun getGoogleDividerText() = composeTestRule.onNodeWithText(
        "Or pay",
        substring = true,
        useUnmergedTree = true
    )

    fun getCardNumber() = composeTestRule.onNodeWithText(
        InstrumentationRegistry.getInstrumentation().targetContext.resources.getString(
            StripeR.string.stripe_acc_label_card_number
        )
    )

    fun getCardExpiration() = composeTestRule.onNodeWithText(
        InstrumentationRegistry.getInstrumentation().targetContext.resources.getString(
            UiCoreR.string.stripe_expiration_date_hint
        )
    )

    fun getCardCvc() = composeTestRule.onNodeWithText(
        InstrumentationRegistry.getInstrumentation().targetContext.resources.getString(
            StripeR.string.stripe_cvc_number_hint
        )
    )

    fun setInitializationType(initializationType: InitializationType) {
        EspressoIdButton(R.id.initialization_type_spinner).click()
        EspressoText(initializationType.value).click()
    }

    fun setCurrency(currency: Currency) {
        EspressoIdButton(R.id.currency_spinner).click()
        EspressoText(currency.name).click()
    }

    fun setMerchantCountry(merchantCountryCode: String) {
        EspressoIdButton(R.id.merchant_country_spinner).click()
        EspressoText(
            CountryUtils.getDisplayCountry(CountryCode(merchantCountryCode), Locale.getDefault())
        ).click()
    }

    fun enterCustomPrimaryButtonLabel(text: String) {
        EspressoEditText(id = R.id.custom_label_text_field).enter(text)
    }

    companion object {
        fun browserWindow(device: UiDevice, browser: BrowserUI): UiObject? =
            device.findObject(
                UiSelector()
                    .packageName(browser.packageName)
                    .resourceId(browser.resourceID)
            )
    }
}
