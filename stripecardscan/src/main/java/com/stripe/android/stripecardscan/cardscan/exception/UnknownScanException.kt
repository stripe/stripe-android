package com.stripe.android.stripecardscan.cardscan.exception

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class UnknownScanException(message: String? = null) : Exception(message)
