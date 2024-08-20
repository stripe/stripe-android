package com.stripe.android.stripeconnect.example.flowselector

import com.stripe.android.stripeconnect.StripeConnectComponent

data class FlowSelectorState(
    val selectedFlow: StripeConnectComponent? = null
) {
    val isLaunchButtonEnabled: Boolean
        get() = selectedFlow != null
}

fun StripeConnectComponent.getDisplayTitle(): String {
    return when (this) {
        StripeConnectComponent.AccountManagement -> "Account management"
        StripeConnectComponent.AccountOnboarding -> "Account onboarding"
        StripeConnectComponent.Documents -> "Documents"
        StripeConnectComponent.Payments -> "Payments"
        StripeConnectComponent.PaymentDetails -> "Payment details"
        StripeConnectComponent.Payouts -> "Payouts"
        StripeConnectComponent.PayoutsList -> "Payouts list"
    }
}