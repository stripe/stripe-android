package com.stripe.android.test.core

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
    val paymentSelection = PaymentSelection(paymentMethod.displayNameResource)
}
