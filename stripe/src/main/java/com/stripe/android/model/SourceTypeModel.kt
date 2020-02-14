package com.stripe.android.model

import androidx.annotation.StringDef
import kotlinx.android.parcel.Parcelize

/**
 * Models for [Source] type-specific data
 */
sealed class SourceTypeModel : StripeModel {
    @Parcelize
    data class Card internal constructor(
        val addressLine1Check: String?,
        val addressZipCheck: String?,
        val brand: CardBrand,
        val country: String?,
        val cvcCheck: String?,
        val dynamicLast4: String?,
        val expiryMonth: Int?,
        val expiryYear: Int?,
        val funding: CardFunding?,
        val last4: String?,
        @ThreeDSecureStatus
        @get:ThreeDSecureStatus
        val threeDSecureStatus: String?,

        val tokenizationMethod: TokenizationMethod? = null
    ) : SourceTypeModel() {
        @Retention(AnnotationRetention.SOURCE)
        @StringDef(ThreeDSecureStatus.REQUIRED, ThreeDSecureStatus.OPTIONAL,
            ThreeDSecureStatus.NOT_SUPPORTED, ThreeDSecureStatus.RECOMMENDED,
            ThreeDSecureStatus.UNKNOWN)
        annotation class ThreeDSecureStatus {
            companion object {
                const val REQUIRED: String = "required"
                const val OPTIONAL: String = "optional"
                const val NOT_SUPPORTED: String = "not_supported"
                const val RECOMMENDED: String = "recommended"
                const val UNKNOWN: String = "unknown"
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
