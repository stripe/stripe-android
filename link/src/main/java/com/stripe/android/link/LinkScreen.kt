package com.stripe.android.link

internal sealed class LinkScreen(val route: String) {
    data object Verification : LinkScreen("verification")
    data object Wallet : LinkScreen("wallet")
    data object PaymentMethod : LinkScreen("paymentMethod")
    data object CardEdit : LinkScreen("cardEdit")
    data object SignUp : LinkScreen("signUp")
}
