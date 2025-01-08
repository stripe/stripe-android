package com.stripe.android.link

internal sealed class LinkScreen(val route: String) {
    data object Loading : LinkScreen("loading")
    data object Verification : LinkScreen("verification")
    data object Wallet : LinkScreen("wallet")
    data object PaymentMethod : LinkScreen("paymentMethod")
    data class CardEdit(val paymentDetailsId: String) : LinkScreen(ROUTE) {
        companion object {
            const val ROUTE = "cardEdit/{paymentDetailsId}"
        }
    }
    data object SignUp : LinkScreen("signUp")
}
