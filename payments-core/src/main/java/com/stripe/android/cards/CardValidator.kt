package com.stripe.android.cards

import android.content.Context
import com.stripe.android.model.CardBrand

class CardValidator(
    context: Context,
    private val cardAccountRangeRepository: CardAccountRangeRepository =
        DefaultCardAccountRangeRepositoryFactory(
            context = context
        ).create()
) {
    suspend fun possibleBrands(cardNumber: String): Set<CardBrand> {
        val ranges = cardAccountRangeRepository.getAccountRanges(
            CardNumber.Unvalidated(cardNumber)
        )
        println(ranges)
        return ranges?.map {
            it.brand
        }?.toSet() ?: setOf()
    }
}