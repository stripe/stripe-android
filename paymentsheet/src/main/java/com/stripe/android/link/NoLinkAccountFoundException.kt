package com.stripe.android.link

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class NoLinkAccountFoundException : IllegalStateException("No Link account found")

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class NoPaymentMethodOptionsAvailable : IllegalStateException("No payment method options available")

internal class NoPaymentDetailsFoundException : IllegalStateException(
    "No payment details associated with this Link account found"
)
