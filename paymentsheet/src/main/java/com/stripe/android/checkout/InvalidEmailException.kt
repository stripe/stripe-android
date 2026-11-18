package com.stripe.android.checkout

internal class InvalidEmailException(
    email: String,
) : IllegalArgumentException("Email '$email' is not a valid email address")
