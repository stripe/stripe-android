package com.stripe.android.model

import com.stripe.android.model.parsers.CustomerSourceJsonParser
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject

/**
 * Model of the "data" object inside a [Customer] "source" object.
 */
@Parcelize
data class CustomerSource internal constructor(
    private val stripePaymentSource: StripePaymentSource
) : StripeModel, StripePaymentSource {

    override val id: String?
        get() = stripePaymentSource.id

    val tokenizationMethod: String?
        get() {
            val paymentAsSource = asSource()
            val paymentAsCard = asCard()
            return if (paymentAsSource != null && Source.SourceType.CARD == paymentAsSource.type) {
                val cardData = paymentAsSource.sourceTypeModel as SourceCardData?
                cardData?.tokenizationMethod
            } else paymentAsCard?.tokenizationMethod
        }

    val sourceType: String
        @Source.SourceType
        get() = when (stripePaymentSource) {
            is Card -> Source.SourceType.CARD
            is Source -> stripePaymentSource.type
            else -> Source.SourceType.UNKNOWN
        }

    fun asSource(): Source? {
        return if (stripePaymentSource is Source) {
            stripePaymentSource
        } else null
    }

    fun asCard(): Card? {
        return if (stripePaymentSource is Card) {
            stripePaymentSource
        } else null
    }

    companion object {
        @JvmStatic
        fun fromJson(jsonObject: JSONObject?): CustomerSource? {
            return jsonObject?.let {
                CustomerSourceJsonParser().parse(jsonObject)
            }
        }
    }
}
