package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ConsumerPaymentDetails(
    val paymentDetails: List<PaymentDetails>
) : StripeModel {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed class PaymentDetails(
        open val id: String,
        open val type: String,
    ) : Parcelable {

        abstract val last4: String
    }

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Card(
        override val id: String,
        override val last4: String,
    ) : PaymentDetails(id, type = "card")

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Passthrough(
        override val id: String,
        override val last4: String,
    ) : PaymentDetails(id, type = "card")

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class BankAccount(
        override val id: String,
        override val last4: String,
        val bankName: String?,
    ) : PaymentDetails(id, type = "bank_account")

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class BillingAddress(
        val countryCode: CountryCode?,
        val postalCode: String?
    ) : Parcelable
}
