package com.stripe.android

import com.stripe.android.paymentsheet.model.SupportedPaymentMethod

data class TestParameters(
    val paymentMethod: SupportedPaymentMethod,
    val customer: Customer,
    val googlePayState: GooglePayState,
    val currency: Currency,
    val checkout: Checkout,
    val billing: Billing,
    val shipping: Shipping,
    val delayed: DelayedPMs,
    val automatic: Automatic,
    val saveCheckboxValue: Boolean,
    val saveForFutureUseCheckboxVisible: Boolean,
    val useBrowser: Browser? = null,
    val authorizationAction: AuthorizeAction? = null
) {
    val paymentSelection = PaymentSelection(paymentMethod.displayNameResource)
}
