package com.stripe.android.test.core.ui

import android.content.pm.PackageManager
import androidx.annotation.StringRes
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
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
import com.stripe.android.model.PaymentMethod.Type.Blik
import com.stripe.android.model.PaymentMethod.Type.CashAppPay
import com.stripe.android.paymentsheet.example.playground.RELOAD_TEST_TAG
import com.stripe.android.paymentsheet.example.playground.settings.CheckoutModeSettingsDefinition
import com.stripe.android.paymentsheet.example.samples.ui.shared.CHECKOUT_TEST_TAG
import com.stripe.android.paymentsheet.example.samples.ui.shared.PAYMENT_METHOD_SELECTOR_TEST_TAG
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.HOOKS_PAGE_LOAD_TIMEOUT
import com.stripe.android.test.core.TestParameters
import com.stripe.android.ui.core.elements.SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG
import kotlin.time.Duration.Companion.seconds
import com.stripe.android.R as StripeR
import com.stripe.android.core.R as CoreR
import com.stripe.android.paymentsheet.R as PaymentSheetR
import com.stripe.android.ui.core.R as PaymentsUiCoreR
import com.stripe.android.uicore.R as UiCoreR

/**
 * This contains the Android specific code such as for accessing UI elements, detecting
 * the installed browsers.  It also abstracts away the testing platform, espresso vs. compose.
 */
internal class Selectors(
    val device: UiDevice,
    val composeTestRule: ComposeTestRule,
    testParameters: TestParameters
) {
    val continueButton = BuyButton(composeTestRule)
    val complete = ComposeButton(composeTestRule, hasTestTag(CHECKOUT_TEST_TAG))
    val reload = ComposeButton(composeTestRule, hasTestTag(RELOAD_TEST_TAG))
    val multiStepSelect = ComposeButton(
        composeTestRule,
        hasTestTag(PAYMENT_METHOD_SELECTOR_TEST_TAG)
    )
    val saveForFutureCheckbox = composeTestRule
        .onNodeWithTag(SAVE_FOR_FUTURE_CHECKBOX_TEST_TAG)

    val paymentSelection = PaymentSelection(
        composeTestRule,
        testParameters.paymentMethod.displayNameResource
    )

    val buyButton = BuyButton(
        composeTestRule = composeTestRule,
        processingCompleteTimeout = if (testParameters.paymentMethod.code == CashAppPay.code) {
            // We're using a longer timeout for Cash App Pay until we fix an issue where we
            // needlessly poll after a canceled payment attempt.
            15.seconds
        } else if (testParameters.paymentMethod.code == Blik.code) {
            30.seconds
        } else {
            5.seconds
        }
    )

    val playgroundBuyButton = ComposeButton(composeTestRule, hasTestTag(CHECKOUT_TEST_TAG))

    val addPaymentMethodButton = AddPaymentMethodButton(device)

    val selectBrowserPrompt = UiAutomatorText("Verify your payment", device = device)

    fun browserIconAtPrompt(browser: BrowserUI) = UiAutomatorText(browser.name, device = device)

    fun browserWindow(browser: BrowserUI): UiObject? = browserWindow(device, browser)

    val closeQrCodeButton = UiAutomatorText("Close", device = device)

    fun blockUntilAuthorizationPageLoaded(isSetup: Boolean) {
        assertThat(
            device.wait(
                Until.findObject(
                    By.textContains("test ${if (isSetup) "setup" else "payment"} page")
                ),
                HOOKS_PAGE_LOAD_TIMEOUT * 1000
            )
        ).isNotNull()
        device.waitForIdle()
    }

    fun blockUntilUSBankAccountPageLoaded() {
        assertThat(
            device.wait(
                Until.findObject(
                    By.textContains("Agree and continue")
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

    private val checkoutMode =
        testParameters.playgroundSettingsSnapshot[CheckoutModeSettingsDefinition]

    @OptIn(ExperimentalTestApi::class)
    val authorizeAction = when (testParameters.authorizationAction) {
        is AuthorizeAction.AuthorizePayment -> {
            object : UiAutomatorText(
                label = testParameters.authorizationAction.text(checkoutMode),
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
                label = testParameters.authorizationAction.text(checkoutMode),
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
                label = testParameters.authorizationAction.text(checkoutMode),
                className = "android.widget.Button",
                device = device
            ) {}
        }

        is AuthorizeAction.Bacs.Confirm -> {
            object : UiAutomatorText(
                label = testParameters.authorizationAction.text(checkoutMode),
                className = "android.widget.Button",
                device = device
            ) {
                override fun click() {
                    composeTestRule.waitUntilExactlyOneExists(
                        hasText(
                            getResourceString(
                                PaymentSheetR.string.stripe_paymentsheet_bacs_mandate_title
                            )
                        )
                    )

                    composeTestRule.onNodeWithText(
                        getResourceString(PaymentSheetR.string.stripe_paymentsheet_confirm)
                    ).performClick()
                }
            }
        }

        is AuthorizeAction.Bacs.ModifyDetails -> {
            object : UiAutomatorText(
                label = testParameters.authorizationAction.text(checkoutMode),
                className = "android.widget.Button",
                device = device
            ) {
                override fun click() {
                    composeTestRule.waitUntilExactlyOneExists(
                        hasText(getResourceString(PaymentSheetR.string.stripe_paymentsheet_bacs_mandate_title))
                    )

                    composeTestRule.onNodeWithText(
                        getResourceString(
                            PaymentSheetR
                                .string
                                .stripe_paymentsheet_bacs_modify_details_button_label
                        )
                    ).performClick()
                }
            }
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

    fun getPostalCode() = composeTestRule.onNodeWithText(
        getResourceString(CoreR.string.stripe_address_label_postal_code)
    )

    fun getAuBsb() = composeTestRule.onNodeWithText(
        getResourceString(StripeR.string.stripe_becs_widget_bsb)
    )

    fun getAuAccountNumber() = composeTestRule.onNodeWithText(
        getResourceString(StripeR.string.stripe_becs_widget_account_number)
    )

    fun getBacsSortCode() = composeTestRule.onNodeWithText(
        getResourceString(PaymentsUiCoreR.string.stripe_bacs_sort_code)
    )

    fun getBacsAccountNumber() = composeTestRule.onNodeWithText(
        getResourceString(StripeR.string.stripe_becs_widget_account_number)
    )

    fun getBacsConfirmed() = composeTestRule.onNode(
        isToggleable().and(hasTestTag("BACS_MANDATE_CHECKBOX"))
    )

    fun getBoletoTaxId() = composeTestRule.onNodeWithText(
        getResourceString(PaymentsUiCoreR.string.stripe_boleto_tax_id_label)
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

    companion object {
        fun browserWindow(device: UiDevice, browser: BrowserUI): UiObject? =
            device.findObject(
                UiSelector()
                    .packageName(browser.packageName)
                    .resourceId(browser.resourceID)
            )
    }
}
