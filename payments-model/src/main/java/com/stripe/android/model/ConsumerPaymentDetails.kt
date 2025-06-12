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
    ) : StripeModel {

        abstract val last4: String
    }

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Card(
        override val id: String,
        override val last4: String,
        override val isDefault: Boolean,
        override val nickname: String?,
        val expiryYear: Int,
        val expiryMonth: Int,
        val brand: CardBrand,
        val networks: List<String>,
        val cvcCheck: CvcCheck,
        val funding: String,
        val billingAddress: BillingAddress? = null,
        val billingEmailAddress: String? = null,
    ) : PaymentDetails(
        id = id,
        isDefault = isDefault,
        type = TYPE,
        nickname = nickname,
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

        companion object {
            const val TYPE = "card"

            /**
             * Reads the address from the [PaymentMethodCreateParams] mapping format to the format
             * used by [ConsumerPaymentDetails].
             */
            internal fun getAddressFromMap(
                cardPaymentMethodCreateParamsMap: Map<String, Any>
            ): Pair<String, Any>? {
                val billingDetails =
                    cardPaymentMethodCreateParamsMap["billing_details"] as? Map<*, *>
                val address = billingDetails?.get("address") as? Map<*, *>
                return address?.let {
                    // card["billing_details"]["address"] becomes card["billing_address"]
                    "billing_address" to mapOf(
                        // card["billing_details"]["address"]["country"]
                        // becomes card["billing_address"]["country_code"]
                        "country_code" to it["country"],
                        "postal_code" to it["postal_code"]
                    )
                }
            }
        }
    }

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Passthrough(
        override val id: String,
        override val last4: String,
    ) : PaymentDetails(
        id = id,
        type = TYPE,
        isDefault = false,
        nickname = null,
    ) {

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
    ) : PaymentDetails(
        id = id,
        type = TYPE,
        isDefault = isDefault,
        nickname = nickname,
    ) {

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
