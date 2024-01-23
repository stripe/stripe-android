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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed class PaymentDetails(
        open val id: String,
        open val type: String
    ) : Parcelable

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Card(
        override val id: String,
        val last4: String,
    ) : PaymentDetails(id, Companion.type) {

        companion object {
            const val type = "card"

            /**
             * Reads the address from the [PaymentMethodCreateParams] mapping format to the format
             * used by [ConsumerPaymentDetails].
             */
            fun getAddressFromMap(
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
        val last4: String,
    ) : PaymentDetails(id, "card")

    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class BankAccount(
        override val id: String,
        val bankIconCode: String?,
        val bankName: String,
        val last4: String
    ) : PaymentDetails(id, Companion.type) {
        companion object {
            const val type = "bank_account"
        }
    }
}
