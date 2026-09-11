package com.stripe.android.crypto.onramp.exception

internal class LinkAccountNotVerifiedException : IllegalStateException(
    "The Link account must be verified before presenting an HTML confirmation."
)
