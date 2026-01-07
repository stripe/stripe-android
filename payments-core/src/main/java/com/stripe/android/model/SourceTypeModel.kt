package com.stripe.android.model

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

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
        val tokenizationMethod: TokenizationMethod? = null
    ) : SourceTypeModel()
}
