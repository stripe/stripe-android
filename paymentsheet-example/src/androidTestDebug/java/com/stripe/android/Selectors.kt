package com.stripe.android

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
import androidx.test.espresso.PerformException
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.playground.model.CheckoutCurrency
import com.stripe.android.paymentsheet.example.playground.model.CheckoutCustomer
import com.stripe.android.paymentsheet.example.playground.model.CheckoutMode
import java.security.InvalidParameterException

open class EspressoLabelIdButton(@StringRes val label: Int) {
    fun click() {
        Espresso.onView(ViewMatchers.withText(label)).perform(ViewActions.click())
    }

    fun exists(): Boolean {
        return try {
            Espresso.onView(ViewMatchers.withText(label))
                .withFailureHandler { _, _ ->
                    throw InvalidParameterException("No payment selector found")
                }
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
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
}

open class UiAutomatorText(
    private val label: String,
    className: String = "android.widget.TextView"
) {
    private val selector = UiSelector().textContains(label)
        .className(className)

    fun click(device: UiDevice) {
        if (!exists(device)) {
            throw InvalidParameterException("Text button not found: $label")
        }
        device.findObject(selector).click()
    }

    fun exists(device: UiDevice) =
        device.findObject(selector).exists()
}

object SaveForFutureCheckbox :
    EspressoLabelIdButton(R.string.stripe_paymentsheet_save_this_card_with_merchant_name)

class PaymentSelection(@StringRes val label: Int) {
    //: com.stripe.android.EspressoLabelIdButton(id) {

    fun exists(): Boolean {
        return try {
            Espresso.onView(ViewMatchers.withId(com.stripe.android.paymentsheet.R.id.payment_methods_recycler))
                .withFailureHandler { _, _ ->
                    throw InvalidParameterException("No payment selector found")
                }
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            true

        } catch (e: InvalidParameterException) {
            false
        }
    }

    fun click(composeTestRule: ComposeTestRule, resource: Resources) {
        try {
            val onNodeWithText = composeTestRule.onNodeWithText(
//                "Card"
                resource.getString(label)
            )
            composeTestRule.onNodeWithTag("PaymentMethodsUI", true)
                .performScrollToNode(hasText(resource.getString(label)))
            onNodeWithText.assertIsDisplayed()
            onNodeWithText.assertIsEnabled()
            onNodeWithText.performClick()

            composeTestRule.waitForIdle()
//            Espresso.onView(ViewMatchers.withId(com.stripe.android.paymentsheet.R.id.payment_methods_recycler))
//                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
//                .perform(
//                    RecyclerViewActions.actionOnHolderItem(
//                        matchPaymentMethodHolder(composeTestRule, resource.getString(label)),
//                        ViewActions.click()
//                    )
//                )
        } catch (e: PerformException) {
            // Item is already selected.
        }
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

sealed class Checkout(@StringRes id: Int) : EspressoLabelIdButton(id) {
    object Pay : Checkout(R.string.payment)
    object PayWithSetup : Checkout(R.string.payment_with_setup)
    object Setup : Checkout(R.string.setup)
}

internal fun Checkout.toRepository() = when (this) {
    Checkout.Pay -> CheckoutMode.Payment
    Checkout.PayWithSetup -> CheckoutMode.PaymentWithSetup
    Checkout.Setup -> CheckoutMode.Setup
}

sealed class Currency(@StringRes id: Int) : EspressoLabelIdButton(id) {
    object USD : Currency(R.string.currency_usd)
    object EUR : Currency(R.string.currency_eur)
}

internal fun Currency.toRepository() = when (this) {
    Currency.USD -> CheckoutCurrency.USD
    Currency.EUR -> CheckoutCurrency.EUR
}

sealed class GooglePayState(@IntegerRes id: Int) : EspressoIdButton(id) {
    object On : GooglePayState(R.id.google_pay_on_button)
    object Off : GooglePayState(R.id.google_pay_off_button)
}

sealed class Customer(@StringRes id: Int) : EspressoLabelIdButton(id) {
    object Guest : Customer(R.string.customer_guest)
    object New : Customer(R.string.customer_new)
}

internal fun Customer.toRepository() = when (this) {
    Customer.Guest -> CheckoutCustomer.Guest
    Customer.New -> CheckoutCustomer.New
}

// No 3DS2 support
sealed class AuthorizeAction(
    name: String,
    className: String
) : UiAutomatorText(name, className) {
    object Authorize : AuthorizeAction("AUTHORIZE TEST PAYMENT", "android.widget.Button")
    object Cancel : AuthorizeAction("", "")
    object Fail : AuthorizeAction("FAIL TEST PAYMENT", "android.widget.Button")
}

object SelectBrowserWindow : UiAutomatorText("Verify your payment")
object AuthorizeWindow {
    fun exists(device: UiDevice, browser: Browser?) =
        device.findObject(getSelector(browser)).exists()

    private fun getSelector(browser: Browser?) = UiSelector()
        .packageName(browser?.packageName)
        .resourceId(browser?.resourceID)
}

object AuthorizePageLoaded {
    fun blockUntilLoaded(device: UiDevice) {
        device.wait(
            Until.findObject(
                By.textContains("test payment page")
            ),
            10000
        )
    }
}

sealed class Browser(name: String, val packageName: String, val resourceID: String) :
    UiAutomatorText(name) {

    object Chrome : Browser("Chrome", "com.android.chrome", "com.android.chrome:id/coordinator")
    object Opera : Browser("Opera", "com.opera.browser", "com.opera.browser:id/action_bar_root")
    object Firefox :
        Browser("Firefox", "org.mozilla.firefox", "org.mozilla.firefox:id/action_bar_root")
}
