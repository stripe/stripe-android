import android.content.res.Resources
import androidx.annotation.IntegerRes
import androidx.annotation.StringRes
import androidx.test.espresso.Espresso
import androidx.test.espresso.PerformException
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.example.repository.Repository
import com.stripe.android.paymentsheet.matchPaymentMethodHolder
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import java.security.InvalidParameterException

open class EspressoLabelIdButton(@StringRes val label: Int) {
    fun click() {
        Espresso.onView(ViewMatchers.withText(label)).perform(ViewActions.click())
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

sealed class PaymentSelection(@StringRes val label: Int) {
    //: EspressoLabelIdButton(id) {
    object Card : PaymentSelection(R.string.stripe_paymentsheet_payment_method_card)
    object Sofort : PaymentSelection(R.string.stripe_paymentsheet_payment_method_sofort)
    object iDEAL : PaymentSelection(R.string.stripe_paymentsheet_payment_method_ideal)
    object Bancontact : PaymentSelection(R.string.stripe_paymentsheet_payment_method_bancontact)
    object SepaDebit : PaymentSelection(R.string.stripe_paymentsheet_payment_method_sepa_debit)
    object Eps : PaymentSelection(R.string.stripe_paymentsheet_payment_method_eps)
    object Giropay : PaymentSelection(R.string.stripe_paymentsheet_payment_method_giropay)
    object P24 : PaymentSelection(R.string.stripe_paymentsheet_payment_method_p24)
    object AfterpayClearPay :
        PaymentSelection(R.string.stripe_paymentsheet_payment_method_afterpay_clearpay)

    fun click(resource: Resources) {
        try {
            Espresso.onView(ViewMatchers.withId(com.stripe.android.paymentsheet.R.id.payment_methods_recycler))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(
                    RecyclerViewActions.actionOnHolderItem(
                        matchPaymentMethodHolder(resource.getString(paymentSelection.label)),
                        ViewActions.click()
                    )
                )
        } catch (e: PerformException) {
            // Item is already selected.
        }

    }

    companion object {
        fun fromString(name: String) =
            when (SupportedPaymentMethod.fromCode(name)?.displayNameResource) {
                Card.label -> Card
                Sofort.label -> Sofort
                iDEAL.label -> iDEAL
                Bancontact.label -> Bancontact
                Eps.label -> Eps
                SepaDebit.label -> SepaDebit
                Giropay.label -> Giropay
                P24.label -> P24
                AfterpayClearPay.label -> AfterpayClearPay
                else -> throw InvalidParameterException("Payment intent payment selection not recognized: $name")
            }
    }
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
    Checkout.Pay -> Repository.CheckoutMode.Payment
    Checkout.PayWithSetup -> Repository.CheckoutMode.PaymentWithSetup
    Checkout.Setup -> Repository.CheckoutMode.Setup
}

sealed class Currency(@StringRes id: Int) : EspressoLabelIdButton(id) {
    object USD : Currency(R.string.currency_usd)
    object EUR : Currency(R.string.currency_eur)
}

internal fun Currency.toRepository() = when (this) {
    Currency.USD -> Repository.CheckoutCurrency.USD
    Currency.EUR -> Repository.CheckoutCurrency.EUR
}

sealed class GooglePayState(@IntegerRes id: Int) : EspressoIdButton(id) {
    object On : GooglePayState(R.id.google_pay_on_button)
    object Off : GooglePayState(R.id.google_pay_off_button)
}

sealed class Customer(@StringRes id: Int) : EspressoLabelIdButton(id) {
    object Guest : Customer(R.string.customer_guest)
    object New : Customer(R.string.customer_new)
    object Returning : Customer(R.string.customer_returning)
}

internal fun Customer.toRepository() = when (this) {
    Customer.Guest -> Repository.CheckoutCustomer.Guest
    Customer.New -> Repository.CheckoutCustomer.New
    Customer.Returning -> Repository.CheckoutCustomer.Returning
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
        .resourceId(browser?.resourdID)
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

sealed class Browser(name: String, val packageName: String) :
    UiAutomatorText(name) {
    val resourdID = "$packageName:id/action_bar_root"

    object Chrome : Browser("Chrome", "com.android.chrome")
    object Opera : Browser("Opera", "com.opera.browser")
    object Firefox : Browser("Firefox", "org.mozilla.firefox")
}
