package com.stripe.android.paymentsheet.ui

import com.stripe.android.CardBrandFilter
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod

internal fun PaymentMethod.Card.getPreferredChoice(): CardBrandChoice {
    return CardBrand.fromCode(displayBrand).toChoice()
}

internal fun PaymentMethod.Card.getAvailableNetworks(cardBrandFilter: CardBrandFilter): List<CardBrandChoice> {
    return networks?.available?.let { brandCodes ->
        brandCodes.map { code ->
            CardBrand.fromCode(code).toChoice()
        }.filter { cardBrandFilter.isAccepted(it.brand) }
    } ?: listOf()
}

private fun CardBrand.toChoice(): CardBrandChoice {
    return CardBrandChoice(brand = this)
}
