package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.model.StripeModel
import com.stripe.android.core.utils.DateUtils
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
        open val nickname: String?,
        open val billingAddress: BillingAddress?,
        open val billingEmailAddress: String?,
    ) : Parcelable {

        abstract val last4: String

        abstract fun withBillingDetails(
            billingAddress: BillingAddress?,
            billingEmailAddress: String?
        ): PaymentDetails
    }

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Card(
        override val id: String,
        override val last4: String,
        override val isDefault: Boolean,
        override val nickname: String?,
        override val billingAddress: BillingAddress? = null,
        override val billingEmailAddress: String? = null,
        val expiryYear: Int,
        val expiryMonth: Int,
        val brand: CardBrand,
        val networks: List<String>,
        val cvcCheck: CvcCheck,
        val funding: String
    ) : PaymentDetails(
        id = id,
        isDefault = isDefault,
        type = TYPE,
        nickname = nickname,
        billingAddress = billingAddress,
        billingEmailAddress = billingEmailAddress
    ) {

        val requiresCardDetailsRecollection: Boolean
            get() = isExpired || cvcCheck.requiresRecollection

        val isExpired: Boolean
            get() = !DateUtils.isExpiryDataValid(
                expiryMonth = expiryMonth,
                expiryYear = expiryYear
            )

        val availableNetworks
            get() = networks
                .map { CardBrand.fromCode(it) }
                .filter { it != CardBrand.Unknown }

        override fun withBillingDetails(
            billingAddress: BillingAddress?,
            billingEmailAddress: String?
        ): PaymentDetails {
            return copy(
                billingAddress = billingAddress,
                billingEmailAddress = billingEmailAddress
            )
        }

        companion object {
            const val TYPE = "card"
        }
    }

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Passthrough(
        override val id: String,
        override val last4: String,
        override val billingAddress: BillingAddress? = null,
        override val billingEmailAddress: String? = null,
    ) : PaymentDetails(
        id = id,
        type = TYPE,
        isDefault = false,
        nickname = null,
        billingAddress = billingAddress,
        billingEmailAddress = billingEmailAddress
    ) {

        override fun withBillingDetails(
            billingAddress: BillingAddress?,
            billingEmailAddress: String?
        ): PaymentDetails {
            return copy(
                billingAddress = billingAddress,
                billingEmailAddress = billingEmailAddress
            )
        }

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
        override val nickname: String?,
        val bankName: String?,
        val bankIconCode: String?,
        override val billingAddress: BillingAddress?,
        override val billingEmailAddress: String?,
    ) : PaymentDetails(
        id = id,
        type = TYPE,
        isDefault = isDefault,
        nickname = nickname,
        billingAddress = billingAddress,
        billingEmailAddress = billingEmailAddress
    ) {

        override fun withBillingDetails(
            billingAddress: BillingAddress?,
            billingEmailAddress: String?
        ): PaymentDetails {
            return copy(
                billingAddress = billingAddress,
                billingEmailAddress = billingEmailAddress
            )
        }

        companion object {
            const val TYPE = "bank_account"
        }
    }

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class BillingAddress(
        val name: String?,
        val line1: String?,
        val line2: String?,
        val administrativeArea: String?,
        val locality: String?,
        val postalCode: String?,
        val countryCode: CountryCode?,
    ) : Parcelable
}
