package com.stripe.android.crypto.onramp.example.model

internal data class CheckoutEvent(
    val sessionId: String
)

internal data class AuthorizeEvent(
    val linkAuthIntentId: String
)
