package com.stripe.android.link

internal sealed class LinkScreen(val route: String) {
    data object Loading : LinkScreen("loading")
    data object Verification : LinkScreen("verification")
    data object Wallet : LinkScreen("wallet")
    data object PaymentMethod : LinkScreen("paymentMethod")
    data class CardEdit(val paymentDetailsId: String) : LinkScreen("cardEdit/$paymentDetailsId}") {
        companion object {
            const val Route = "cardEdit/{paymentDetailsId}"
        }
    }
    data object SignUp : LinkScreen("signUp")
}
