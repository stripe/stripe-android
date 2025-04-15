package com.stripe.android.paymentsheet.ui

import com.stripe.android.CardBrandFilter
import com.stripe.android.model.CardBrand

internal fun EditCardPayload.getPreferredChoice(cardBrandFilter: CardBrandFilter): CardBrandChoice {
    return CardBrand.fromCode(displayBrand).toChoice(cardBrandFilter)
}

internal fun EditCardPayload.getAvailableNetworks(cardBrandFilter: CardBrandFilter): List<CardBrandChoice> {
    return networks?.let { brandCodes ->
        brandCodes.map { code ->
            CardBrand.fromCode(code).toChoice(cardBrandFilter)
        }
    } ?: listOf()
}

private fun CardBrand.toChoice(cardBrandFilter: CardBrandFilter): CardBrandChoice {
    return CardBrandChoice(brand = this, enabled = cardBrandFilter.isAccepted(this))
}
