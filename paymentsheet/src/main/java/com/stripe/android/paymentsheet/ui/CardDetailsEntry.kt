package com.stripe.android.paymentsheet.ui

import com.stripe.android.paymentsheet.CardUpdateParams

internal data class CardDetailsEntry(
    val cardBrandChoice: CardBrandChoice,
) {
    fun hasChanged(
        cardBrandChoice: CardBrandChoice,
    ): Boolean {
        return cardBrandChoice != this.cardBrandChoice
    }
}

internal fun CardDetailsEntry.toUpdateParams(): CardUpdateParams {
    return CardUpdateParams(cardBrand = cardBrandChoice.brand)
}
