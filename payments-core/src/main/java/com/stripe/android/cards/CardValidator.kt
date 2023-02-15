package com.stripe.android.cards

import android.content.Context
import com.stripe.android.model.CardBrand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Class to validate cards.
 * @param context The Android [Context]
 */
class CardValidator(
    context: Context,
    private val cardAccountRangeRepository: CardAccountRangeRepository =
        DefaultCardAccountRangeRepositoryFactory(
            context = context
        ).create()
) {
    /**
     * Returns available brands for the provided card number
     * @param cardNumber the card number to retrieve possible brands
     * @return a set of possible [CardBrand]
     */
    suspend fun possibleBrands(cardNumber: String): Set<CardBrand> {
        val ranges = cardAccountRangeRepository.getAccountRanges(
            CardNumber.Unvalidated(cardNumber)
        )
        return ranges?.map {
            it.brand
        }?.toSet() ?: setOf()
    }

    /**
     * Returns available brands for the provided card number
     * @param cardNumber the card number to retrieve possible brands
     * @param onSuccess the callback called when the possible brands are retrieved
     * @param onFailure the callback called when an exception has happened
     * @return a set of possible [CardBrand]
     */
    fun possibleBrands(
        cardNumber: String,
        onSuccess: (Set<CardBrand>) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val ranges = cardAccountRangeRepository.getAccountRanges(
                    CardNumber.Unvalidated(cardNumber)
                )
                onSuccess(
                    ranges?.map {
                        it.brand
                    }?.toSet() ?: setOf()
                )
            } catch (ex: Exception) {
                onFailure(ex)
            }
        }
    }
}