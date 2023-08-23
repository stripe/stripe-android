package com.stripe.android.core.exception

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LocalStripeException(
    val displayMessage: String?
) : StripeException(
    message = displayMessage
)
