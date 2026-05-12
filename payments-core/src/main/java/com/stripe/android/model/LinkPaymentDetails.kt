package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface LinkPaymentDetails : Parcelable {

    val last4: String

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Card(
        val nickname: String?,
        val expMonth: Int,
        val expYear: Int,
        override val last4: String,
        val brand: CardBrand,
        val funding: String,
    ) : LinkPaymentDetails

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class BankAccount(
        val bankName: String?,
        override val last4: String,
    ) : LinkPaymentDetails

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Unknown(
        val nickname: String?,
        val label: String?,
        val sublabel: String?,
        val icon: ConsentUi.Icon?,
        override val last4: String
    ) : LinkPaymentDetails
}
