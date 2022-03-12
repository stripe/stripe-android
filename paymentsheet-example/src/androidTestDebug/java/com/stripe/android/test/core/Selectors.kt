package com.stripe.android.test.core

import android.content.res.Resources
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

abstract class EspressoIdButton(@IntegerRes val id: Int) {
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

    fun hasText(text: String) {
        Espresso.onView(ViewMatchers.withId(id))
            .check(matches(ViewMatchers.withText(text)))
    }
}

open class UiAutomatorText(
    private val label: String,
    var className: String? = "android.widget.TextView"
) {
    val selector: UiSelector
        get() = className?.let {
            UiSelector().textContains(label).className(className)
        } ?: UiSelector().textContains(label)

    open fun click(device: UiDevice) {
        if (!exists(device)) {
            throw InvalidParameterException("Text button not found: $label $className")
        }
        device.findObject(selector).click()
    }

    fun exists(device: UiDevice) =
        device.findObject(selector).exists()

    fun wait(device: UiDevice, waitMs: Long) =
        device.wait(Until.findObject(By.text(label)), waitMs)
}

object PlaygroundBuyButton : EspressoIdButton(R.id.buy_button) {
    fun waitProcessingComplete(device: UiDevice) {
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

object SaveForFutureCheckbox :
    LabelIdButton(R.string.stripe_paymentsheet_save_this_card_with_merchant_name)

class PaymentSelection(@StringRes val label: Int) {
    fun click(composeTestRule: ComposeTestRule, resource: Resources) {
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

sealed class Automatic(@IntegerRes id: Int) : EspressoIdButton(id) {
    object On : Automatic(R.id.automatic_pm_on_button)
    object Off : Automatic(R.id.automatic_pm_off_button)
}

sealed class DelayedPMs(@IntegerRes id: Int) : EspressoIdButton(id) {
    object On : DelayedPMs(R.id.allowsDelayedPaymentMethods_on_button)
    object Off : DelayedPMs(R.id.allowsDelayedPaymentMethods_off_button)
}

sealed class Billing(@IntegerRes id: Int) : EspressoIdButton(id) {
    object On : Billing(R.id.default_billing_on_button)
    object Off : Billing(R.id.default_billing_off_button)
}

sealed class Shipping(@IntegerRes id: Int) : EspressoIdButton(id) {
    object On : Shipping(R.id.shipping_on_button)
    object Off : Shipping(R.id.shipping_off_button)
}

sealed class Checkout(@StringRes id: Int) : LabelIdButton(id) {
    object Pay : Checkout(R.string.payment)
    object PayWithSetup : Checkout(R.string.payment_with_setup)
    object Setup : Checkout(R.string.setup)
}

sealed class Currency(@StringRes id: Int) : LabelIdButton(id) {
    object USD : Currency(R.string.currency_usd)
    object EUR : Currency(R.string.currency_eur)
}

sealed class GooglePayState(@IntegerRes id: Int) : EspressoIdButton(id) {
    object On : GooglePayState(R.id.google_pay_on_button)
    object Off : GooglePayState(R.id.google_pay_off_button)
}

sealed class Customer(@StringRes id: Int) : LabelIdButton(id) {
    object Guest : Customer(R.string.customer_guest)
    object New : Customer(R.string.customer_new)
}


sealed class AuthorizeAction(
    name: String,
    className: String
) : UiAutomatorText(name, className) {
    // TODO: Do these get localized?
    object Authorize : AuthorizeAction(
        "AUTHORIZE TEST PAYMENT",
        "android.widget.Button"
    )

    object Fail : AuthorizeAction(
        "FAIL TEST PAYMENT",
        "android.widget.Button"
    )

    override fun click(device: UiDevice) {
        // Afterpay, giropay and some other authorization test pages
        // have their authorization class name of TextView instead of button,
        // in other cases such as bancontact and ideal the authorize class prevents
        // it from finding the button.  More investigation into why is needed.
        if (!exists(device)) {
            className = null
        }

        super.click(device)
    }

    object Cancel : AuthorizeAction("", "") {
        override fun click(device: UiDevice) {
            device.pressBack()
        }
    }
}

object SelectBrowserWindow : UiAutomatorText("Verify your payment")
object AuthorizeWindow {
    fun exists(device: UiDevice, browser: Browser) =
        device.findObject(getSelector(browser)).exists()

    private fun getSelector(browser: Browser) = UiSelector()
        .packageName(browser.packageName)
        .resourceId(browser.resourceID)
}

object AuthorizePageLoaded {
    fun blockUntilLoaded(device: UiDevice) {
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
}

sealed class Browser(name: String, val packageName: String, val resourceID: String) :
    UiAutomatorText(name) {

    object Chrome : Browser(
        "Chrome",
        "com.android.chrome",
        "com.android.chrome:id/coordinator"
    )

    object Firefox : Browser(
        "Firefox",
        "org.mozilla.firefox",
        "org.mozilla.firefox:id/action_bar_root"
    )

    companion object {
        fun to(name: String) = when (name) {
            Firefox.packageName -> Firefox
            Chrome.packageName -> Chrome
            else -> null
        }
    }
}
