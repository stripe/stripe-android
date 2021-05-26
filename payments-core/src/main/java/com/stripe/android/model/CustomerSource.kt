package com.stripe.android.model

import kotlinx.parcelize.Parcelize

/**
 * Model of the "data" object inside a [Customer] "source" object.
 */
@Parcelize
data class CustomerSource internal constructor(
    private val stripePaymentSource: StripePaymentSource
) : StripeModel, StripePaymentSource {

    override val id: String?
        get() = stripePaymentSource.id

    val tokenizationMethod: TokenizationMethod?
        get() {
            return asSource()?.let { source ->
                when (source.sourceTypeModel) {
                    is SourceTypeModel.Card -> {
                        source.sourceTypeModel.tokenizationMethod
                    }
                    else -> null
                }
            } ?: asCard()?.tokenizationMethod
        }

    val sourceType: String
        @Source.SourceType
        get() = when (stripePaymentSource) {
            is Card -> Source.SourceType.CARD
            is Source -> stripePaymentSource.type
            else -> Source.SourceType.UNKNOWN
        }

    fun asSource(): Source? {
        return stripePaymentSource as? Source?
    }

    fun asCard(): Card? {
        return stripePaymentSource as? Card?
    }
}
