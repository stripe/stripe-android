package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ScannedCardDetails(
    val cardNumber: String,
    val expirationYear: Int?,
    val expirationMonth: Int?
)
