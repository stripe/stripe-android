package com.stripe.android.test.core

import android.content.pm.PackageManager
import androidx.annotation.IntegerRes
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.TEST_TAG_LIST
import com.stripe.android.paymentsheet.example.R
import java.security.InvalidParameterException

open class EspressoText(private val text: String) {
    fun isDisplayed() {
        Espresso.onView(ViewMatchers.withText(text))
            .check(matches(ViewMatchers.isDisplayed()))
    }
}

open class LabelIdButton(@StringRes val label: Int) {
    fun click() {
        Espresso.onView(ViewMatchers.withText(label))
            .perform(ViewActions.scrollTo())
            .perform(ViewActions.click())
    }

    fun exists(): Boolean {
        return try {
            Espresso.onView(ViewMatchers.withText(label))
                .withFailureHandler { _, _ ->
                    throw InvalidParameterException("No payment selector found")
                }
                .check(matches(ViewMatchers.isDisplayed()))
            true
        } catch (e: InvalidParameterException) {
            false
        }
    }
}

open class EspressoIdButton(@IntegerRes val id: Int) {
    fun click() {
        Espresso.onView(ViewMatchers.withId(id)).perform(ViewActions.click())
    }

    fun scrollTo() {
        Espresso.onView(ViewMatchers.withId(id)).perform(ViewActions.scrollTo())
    }

    fun isEnabled() {
        Espresso.onView(ViewMatchers.withId(id))
            .check(matches(ViewMatchers.isEnabled()))
    }

    fun isDisplayed() {
        Espresso.onView(ViewMatchers.withId(id))
            .check(matches(ViewMatchers.isDisplayed()))
    }
}

open class UiAutomatorText(
    private val label: String,
    var className: String? = "android.widget.TextView",
    private val device: UiDevice
) {
    val selector: UiSelector
        get() = className?.let {
            UiSelector().textContains(label).className(className)
        } ?: UiSelector().textContains(label)

    open fun click() {
        if (!exists()) {
            throw InvalidParameterException("Text button not found: $label $className")
        }
        device.findObject(selector).click()
    }

    fun exists() = device.findObject(selector).exists()

    fun wait(waitMs: Long) =
        device.wait(Until.findObject(By.text(label)), waitMs)
}

class Selectors(
    val device: UiDevice,
    val composeTestRule: ComposeTestRule,
    testParameters: TestParameters
) {

    val customer = when (testParameters.customer) {
        Customer.Guest -> LabelIdButton(R.string.customer_guest)
        Customer.New -> LabelIdButton(R.string.customer_new)
    }
    val googlePayState = when (testParameters.googlePayState) {
        GooglePayState.Off -> EspressoIdButton(R.id.google_pay_off_button)
        GooglePayState.On -> EspressoIdButton(R.id.google_pay_on_button)
    }
    val currency = when (testParameters.currency) {
        Currency.EUR -> LabelIdButton(R.string.currency_eur)
        Currency.USD -> LabelIdButton(R.string.currency_usd)
    }

    val checkout = when (testParameters.checkout) {
        Checkout.Pay -> LabelIdButton(R.string.payment)
        Checkout.PayWithSetup -> LabelIdButton(R.string.payment_with_setup)
        Checkout.Setup -> LabelIdButton(R.string.setup)
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
        testParameters.checkout.name

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
    fun getEmail() = composeTestRule.onNodeWithText(getResourceString(R.string.email))
    fun getName() = composeTestRule.onNodeWithText(getResourceString(R.string.address_label_name))
    fun getLine1() =
        composeTestRule.onNodeWithText(getResourceString(R.string.address_label_address_line1))

    fun getLine2() =
        composeTestRule.onNodeWithText(getResourceString(R.string.address_label_address_line2))

    fun getCity() = composeTestRule.onNodeWithText(getResourceString(R.string.address_label_city))
    fun getState() = composeTestRule.onNodeWithText(getResourceString(R.string.address_label_state))
    fun getZip() =
        composeTestRule.onNodeWithText(getResourceString(R.string.address_label_zip_code))

    companion object {
        fun browserWindow(device: UiDevice, browser: BrowserUI): UiObject? =
            device.findObject(
                UiSelector()
                    .packageName(browser.packageName)
                    .resourceId(browser.resourceID)
            )
    }
}

sealed class BrowserUI(val name: String, val packageName: String, val resourceID: String) {
    object Chrome : BrowserUI(
        "Chrome",
        "com.android.chrome",
        "com.android.chrome:id/coordinator"
    )

    object Firefox : BrowserUI(
        "Firefox",
        "org.mozilla.firefox",
        "org.mozilla.firefox:id/action_bar_root"
    )

    companion object {
        fun values() = setOf(Chrome, Firefox)
        fun convert(browser: Browser?): BrowserUI? {
            return when (browser) {
                Browser.Chrome -> Chrome
                Browser.Firefox -> Firefox
                else -> null
            }
        }
    }
}

class BuyButton(private val device: UiDevice) : EspressoIdButton(R.id.buy_button) {
    fun waitProcessingComplete() {
        device.wait(
            Until.findObject(
                By.textContains(
                    InstrumentationRegistry.getInstrumentation().targetContext.resources.getString(
                        com.stripe.android.paymentsheet.R.string.stripe_paymentsheet_pay_button_amount
                    )
                )
            ),
            InstrumentationRegistry.getInstrumentation().targetContext.resources
                .getInteger(android.R.integer.config_shortAnimTime)
                .toLong()
        )
    }
}

class PaymentSelection(val composeTestRule: ComposeTestRule, @StringRes val label: Int) {
    fun click() {
        val resource = InstrumentationRegistry.getInstrumentation().targetContext.resources
        composeTestRule.onNodeWithTag(TEST_TAG_LIST, true)
            .performScrollToNode(hasText(resource.getString(label)))
        composeTestRule
            .onNodeWithText(resource.getString(label))
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        composeTestRule.waitForIdle()
    }
}
