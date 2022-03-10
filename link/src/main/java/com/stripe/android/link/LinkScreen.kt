package com.stripe.android.link

/**
 * All Link screens.
 */
internal sealed class LinkScreen(
    val route: String
) {
    object Loading : LinkScreen("Loading")
    object SignUp : LinkScreen("SignUp")
    object Verification : LinkScreen("Verification")
    object Wallet : LinkScreen("Wallet")
    object AddPaymentMethod : LinkScreen("AddPaymentMethod")
}
