package com.stripe.android.link

import android.os.Bundle
import androidx.core.os.bundleOf

internal sealed class LinkScreen(val route: String) {
    open fun resolveRoute(): String {
        return route
    }
    data object Loading : LinkScreen("loading")
    data object Verification : LinkScreen("verification")
    data object Wallet : LinkScreen("wallet")
    data object PaymentMethod : LinkScreen("paymentMethod")
    data class CardEdit(val paymentDetailsId: String) : LinkScreen(ROUTE) {
        override fun resolveRoute(): String {
            return "cardEdit/$paymentDetailsId"
        }

        companion object {
            const val ROUTE = "cardEdit/{paymentDetailsId}"
        }
    }

    data object SignUp : LinkScreen("signUp")
}
