package com.stripe.android.link.ui.wallet

import com.stripe.android.CardBrandFilter
import com.stripe.android.model.CardBrand
import kotlinx.parcelize.Parcelize

@Parcelize
class RejectCardBrands(
    private val cardBrands: Set<CardBrand>
) : CardBrandFilter {
    constructor(vararg cardBrands: CardBrand) : this(cardBrands.toSet())

    override fun isAccepted(cardBrand: CardBrand): Boolean {
        return cardBrand !in cardBrands
    }
}
