package com.stripe.android.cards

import com.stripe.android.model.CardBrand

internal interface CardBrandSource {
    fun getCardBrand(cardNumber: String): CardBrand
}
