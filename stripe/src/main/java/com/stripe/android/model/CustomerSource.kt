package com.stripe.android.model

import kotlinx.android.parcel.Parcelize

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
                cardData?.tokenizationMethod?.code
            } else {
                paymentAsCard?.tokenizationMethod?.code
            }
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
}
