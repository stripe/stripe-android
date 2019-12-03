package com.stripe.android.model

import androidx.annotation.StringDef
import com.stripe.android.model.parsers.SourceCardDataJsonParser
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject

/**
 * Model for data contained in the SourceTypeData of a Card Source.
 */
@Parcelize
data class SourceCardData internal constructor(
    val addressLine1Check: String?,
    val addressZipCheck: String?,

    @Card.CardBrand
    @get:Card.CardBrand
    val brand: String?,

    val country: String?,
    val cvcCheck: String?,
    val dynamicLast4: String?,
    val expiryMonth: Int?,
    val expiryYear: Int?,

    @Card.FundingType
    @get:Card.FundingType
    val funding: String?,

    val last4: String?,
    @ThreeDSecureStatus
    @get:ThreeDSecureStatus
    val threeDSecureStatus: String?,

    val tokenizationMethod: String?
) : StripeSourceTypeModel() {
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

    companion object {
        @JvmStatic
        fun fromJson(jsonObject: JSONObject?): SourceCardData? {
            return jsonObject?.let {
                SourceCardDataJsonParser().parse(it)
            }
        }
    }
}
