package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.utils.DateUtils
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
        open val isDefault: Boolean,
        open val type: String,
    ) : Parcelable {

        abstract val last4: String
    }

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Card(
        override val id: String,
        override val last4: String,
        override val isDefault: Boolean,
        val expiryYear: Int,
        val expiryMonth: Int,
        val brand: CardBrand,
        val cvcCheck: CvcCheck,
        val billingAddress: BillingAddress? = null
    ) : PaymentDetails(
        id = id,
        isDefault = isDefault,
        type = TYPE
    ) {

        val requiresCardDetailsRecollection: Boolean
            get() = isExpired || cvcCheck.requiresRecollection

        val isExpired: Boolean
            get() = !DateUtils.isExpiryDataValid(
                expiryMonth = expiryMonth,
                expiryYear = expiryYear
            )

        companion object {
            const val TYPE = "card"
        }
    }

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Passthrough(
        override val id: String,
        override val last4: String,
    ) : PaymentDetails(id = id, type = TYPE, isDefault = false) {
        companion object {
            const val TYPE = "card"
        }
    }

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class BankAccount(
        override val id: String,
        override val last4: String,
        override val isDefault: Boolean,
        val bankName: String?,
        val bankIconCode: String?,
    ) : PaymentDetails(
        id = id,
        type = TYPE,
        isDefault = isDefault
    ) {
        companion object {
            const val TYPE = "bank_account"
        }
    }

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class BillingAddress(
        val countryCode: CountryCode?,
        val postalCode: String?
    ) : Parcelable
}
