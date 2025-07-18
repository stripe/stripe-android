package com.stripe.android.link

internal class NoLinkAccountFoundException : IllegalStateException("No Link account found")
internal class NoPaymentDetailsFoundException : IllegalStateException(
    "No payment details associated with this Link account found"
)
