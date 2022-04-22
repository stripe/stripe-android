package com.stripe.android.test.core

import com.stripe.android.paymentsheet.model.SupportedPaymentMethod

/**
 * This is the data class that represents the parameters used to run the test.
 */
data class TestParameters(
    val paymentMethod: SupportedPaymentMethod,
    val customer: Customer,
    val googlePayState: GooglePayState,
    val currency: Currency,
    val intentType: IntentType,
    val billing: Billing,
    val shipping: Shipping,
    val delayed: DelayedPMs,
    val automatic: Automatic,
    val saveCheckboxValue: Boolean,
    val saveForFutureUseCheckboxVisible: Boolean,
    val useBrowser: Browser? = null,
    val authorizationAction: AuthorizeAction? = null,
    val takeScreenshotOnLpmLoad: Boolean = false
)

/**
 * Indicates if automatic payment methods are used on the payment intent
 */
enum class Automatic {
    On,
    Off
}

/**
 * Indicates if delayed payment methods are used on the payment intent
 */
enum class DelayedPMs {
    On,
    Off
}

/**
 * Indicates if default billing details should be provided in the PaymentSheet.Configuration.
 * Setting this to on will make the test run faster so not as many fields
 * need to be filled out.
 */
enum class Billing {
    On,
    Off
}

/**
 * Indicates if shipping should be provided on the payment intent
 */
enum class Shipping {
    On,
    Off
}

/**
 * Indicates a specific browser is required for the test.
 */
enum class Browser {
    Chrome,
    Firefox
}

/**
 * Indicate the payment method for this test expects authorization and how the authorization
 * should be handled: complete, fail, cancel
 */
enum class AuthorizeAction(
    val text: String,
) {
    // These do not get localized.
    Authorize("AUTHORIZE TEST PAYMENT"),
    Fail("FAIL TEST PAYMENT"),
    Cancel("")
}

/**
 * Indicates how the payment intent should be set: PaymentIntent, PaymentIntent with
 * setup for future usage set, or SetupIntent
 */
enum class IntentType {
    Pay,
    PayWithSetup,
    Setup,
}

/**
 * Indicates the currency to use on teh PaymentIntent
 */
enum class Currency {
    USD,
    EUR
}

/**
 * Indicates the state of Google Pay in the PaymentSheet Configuration
 */
enum class GooglePayState {
    On,
    Off
}

/**
 * Indicates the type of customer to use.
 */
enum class Customer {
    Guest,
    New
}
