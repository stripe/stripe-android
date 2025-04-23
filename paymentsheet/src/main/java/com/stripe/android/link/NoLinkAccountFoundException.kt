package com.stripe.android.link

internal class NoLinkAccountFoundException : IllegalStateException("No link account found")
internal class NoPaymentDetailsFoundException : IllegalStateException("No payment details associated with this link account found")
