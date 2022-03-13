package com.stripe.android.test.core

import com.stripe.android.paymentsheet.example.R
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod

data class TestParameters(
    val paymentMethod: SupportedPaymentMethod,
    val customer: Customer,
    val googlePayState: GooglePayState,
    val currency: Currency,
    val checkout: Checkout,// TODO: Rename this enum not to confuse with checkout
    val billing: Billing,
    val shipping: Shipping,
    val delayed: DelayedPMs,
    val automatic: Automatic,
    val saveCheckboxValue: Boolean,
    val saveForFutureUseCheckboxVisible: Boolean,
    val useBrowser: Browser? = null,
    val authorizationAction: AuthorizeAction? = null,
    val takeScreenshotOnLpmLoad: Boolean = false
) {
}

object SaveForFutureCheckbox :
    LabelIdButton(R.string.stripe_paymentsheet_save_this_card_with_merchant_name)


enum class Automatic {
    On,
    Off
}

enum class DelayedPMs {
    On,
    Off
}

enum class Billing {
    On,
    Off
}

enum class Shipping {
    On,
    Off
}

enum class Browser {
    Chrome,
    Firefox
}

enum class AuthorizeAction(
    val text: String,
) {
    // TODO: Do these get localized?
    Authorize("AUTHORIZE TEST PAYMENT"),
    Fail("FAIL TEST PAYMENT"),
    Cancel("")
}

enum class Checkout {
    Pay,
    PayWithSetup,
    Setup,
}

enum class Currency {
    USD,
    EUR
}

enum class GooglePayState {
    On,
    Off
}

enum class Customer {
    Guest,
    New
}
