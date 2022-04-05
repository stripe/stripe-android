package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ConsumerPaymentDetails internal constructor(
    val paymentDetails: List<PaymentDetails>
) : StripeModel {

    sealed class PaymentDetails(
        open val id: String,
        open val isDefault: Boolean
    ) : Parcelable

    @Parcelize
    data class Card(
        override val id: String,
        override val isDefault: Boolean,
        val expiryYear: Int,
        val expiryMonth: Int,
        val brand: CardBrand,
        val last4: String
    ) : PaymentDetails(id, isDefault) {
        companion object {
            const val type = "card"
        }
    }
}
