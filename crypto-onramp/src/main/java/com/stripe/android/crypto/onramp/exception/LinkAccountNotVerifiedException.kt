package com.stripe.android.crypto.onramp.exception

internal class LinkAccountNotVerifiedException : IllegalStateException(
    "The request requires a verified Link account session"
)
