package com.stripe.android.model

import kotlinx.android.parcel.Parcelize

/**
 * Models for [Source] type-specific data
 */
sealed class SourceTypeModel : StripeModel {
    @Parcelize
    data class Card internal constructor(
        val addressLine1Check: String? = null,
        val addressZipCheck: String? = null,
        val brand: CardBrand,
        val country: String? = null,
        val cvcCheck: String? = null,
        val dynamicLast4: String? = null,
        val expiryMonth: Int? = null,
        val expiryYear: Int? = null,
        val funding: CardFunding? = null,
        val last4: String? = null,
        val threeDSecureStatus: ThreeDSecureStatus? = null,
        val tokenizationMethod: TokenizationMethod? = null
    ) : SourceTypeModel() {
        enum class ThreeDSecureStatus(private val code: String) {
            Required("required"),
            Optional("optional"),
            NotSupported("not_supported"),
            Recommended("recommended"),
            Unknown("unknown");

            override fun toString(): String = code

            internal companion object {
                fun fromCode(code: String?) = values().firstOrNull { it.code == code }
            }
        }
    }

    @Parcelize
    data class SepaDebit internal constructor(
        val bankCode: String?,
        val branchCode: String?,
        val country: String?,
        val fingerPrint: String?,
        val last4: String?,
        val mandateReference: String?,
        val mandateUrl: String?
    ) : SourceTypeModel()
}
