package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface ScannedCardDetails {
    val cardNumber: String
    val expirationYear: Int?
    val expirationMonth: Int?

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Unvalidated(
        override val cardNumber: String,
        override val expirationYear: Int?,
        override val expirationMonth: Int?,
    ) : ScannedCardDetails

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Validated(
        override val cardNumber: String,
        override val expirationYear: Int,
        override val expirationMonth: Int,
    ) : ScannedCardDetails
}
