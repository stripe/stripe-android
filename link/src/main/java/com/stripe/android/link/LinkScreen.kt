package com.stripe.android.link

internal sealed interface LinkScreen {
    data object Verification : LinkScreen
    data object Wallet : LinkScreen
    data object PaymentMethod : LinkScreen
    data object CardEdit : LinkScreen
    data object SignUp : LinkScreen
}
