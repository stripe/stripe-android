package com.stripe.android.crypto.onramp.exception

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class MissingCryptoCustomerException : IllegalStateException("Missing crypto customer ID")
