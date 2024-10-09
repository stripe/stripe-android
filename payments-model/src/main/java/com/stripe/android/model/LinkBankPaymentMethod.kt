package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This is a partial copy of [com.stripe.android.model.PaymentMethod]. The Financial Connections SDK doesn't have access
 * to it, so we use this object to transport the payment method data from the Financial Connections SDK to the
 * Payments SDK.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Parcelize
@Serializable
data class LinkBankPaymentMethod(
    @SerialName("id") val id: String,
    @SerialName("allow_redisplay") val allowRedisplay: String? = null,
    @SerialName("billing_details") val billingDetails: BillingDetails? = null,
    @SerialName("created") val created: Long? = null,
    @SerialName("customer") val customer: String? = null,
    @SerialName("livemode") val livemode: Boolean,
    @SerialName("card") val card: Card? = null,
) : StripeModel {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    @Serializable
    data class Card(
        val brand: CardBrand = CardBrand.Unknown,
        val checks: Checks? = null,
        val country: String? = null,
        val expiryMonth: Int? = null,
        val expiryYear: Int? = null,
        val fingerprint: String? = null,
        val funding: String? = null,
        val last4: String? = null,
        val threeDSecureUsage: ThreeDSecureUsage? = null,
        val networks: Networks? = null,
    ) : StripeModel {

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Parcelize
        @Serializable
        data class Checks(
            val addressLine1Check: String?,
            val addressPostalCodeCheck: String?,
            val cvcCheck: String?,
        ) : StripeModel

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Parcelize
        @Serializable
        data class ThreeDSecureUsage(
            val isSupported: Boolean,
        ) : StripeModel

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Parcelize
        @Serializable
        data class Networks(
            val available: Set<String> = emptySet(),
            val preferred: String? = null,
        ) : StripeModel
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    @Serializable
    data class BillingDetails(
        val address: Address? = null,
        val email: String? = null,
        val name: String? = null,
        val phone: String? = null,
    ) : Parcelable

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    @Serializable
    data class Address(
        val city: String? = null,
        val country: String? = null, // two-character country code
        val line1: String? = null,
        val line2: String? = null,
        val postalCode: String? = null,
        val state: String? = null
    ) : Parcelable
}
