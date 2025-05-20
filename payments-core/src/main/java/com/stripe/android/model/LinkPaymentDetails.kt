package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface LinkPaymentDetails : Parcelable {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Card(
        val expMonth: Int,
        val expYear: Int,
        val last4: String,
        val brand: CardBrand,
    ) : LinkPaymentDetails

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class BankAccount(
        val bankName: String?,
        val last4: String,
    ) : LinkPaymentDetails
}
