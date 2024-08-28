package com.stripe.android.stripeconnect.example.flowselector

import com.stripe.android.stripeconnect.Appearance
import com.stripe.android.stripeconnect.StripeConnectComponent

data class FlowSelectorState(
    val selectedFlow: StripeConnectComponent? = StripeConnectComponent.Payouts,
    val selectedAccount: StripeAccount? = StripeAccount.Custom,
    val selectedAppearance: Pair<Appearance?, String>? = null to "Default"
) {
    val isLaunchButtonEnabled: Boolean
        get() = selectedFlow != null && selectedAccount != null && selectedAppearance != null
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

enum class StripeAccount(val displayTitle: String, val accountId: String) {
    Express("Express", "acct_1MhgrJPu4nAj1Tce"),
    StandardCbspSSS("Standard CBSP (SSS)", "acct_1NBR5cQ55yzNh0Wh"),
    Custom("Custom", "acct_1N9FIXQ26HdRlxHg"),
    Custom_Simond_3("Custom simond+3", "acct_1PqeteQ16Tobs4G2"),
    MaxsAccountPCI("Max's account (with PCI)", "acct_1PHAcmPqW2nb5bJu"),
    SnsUA7("SNS (UA7 with Stripe owns pricing)", "acct_1OxEPrQ4YLD3lwUp"),
    MaxTestCameraAccount("Max's test camera account", "acct_1PsT3iQ8UerQD6eV"),
}
