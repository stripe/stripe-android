package com.stripe.android.link

/**
 * All Link screens.
 */
internal sealed class LinkScreen(
    open val route: String
) {
    object VerificationDialog : LinkScreen("VerificationDialog")
    object Wallet : LinkScreen("Wallet")
}
