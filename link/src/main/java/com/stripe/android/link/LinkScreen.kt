package com.stripe.android.link

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * All Link screens.
 */
internal sealed class LinkScreen(
    open val route: String
) {
    object Loading : LinkScreen("Loading")
    object Verification : LinkScreen("Verification")
    object Wallet : LinkScreen("Wallet")
    object PaymentMethod : LinkScreen("PaymentMethod")

    class CardEdit(paymentDetailsId: String) :
        LinkScreen("CardEdit?$idArg=${paymentDetailsId.urlEncode()}") {
        override val route = super.route

        companion object {
            const val idArg = "id"
            const val route = "CardEdit?$idArg={$idArg}"
        }
    }

    class SignUp(email: String? = null) :
        LinkScreen("SignUp${email?.let { "?$emailArg=${it.urlEncode()}" } ?: ""}") {
        override val route = super.route

        companion object {
            const val emailArg = "email"
            const val route = "SignUp?$emailArg={$emailArg}"
        }
    }
}

private fun String.urlEncode(): String =
    URLEncoder.encode(this, StandardCharsets.UTF_8.toString())
