package com.stripe.android.cards

import com.stripe.android.cards.CardAccountRangeSource.Companion.BIN_LENGTH
import com.stripe.android.model.CardMetadata

internal class InMemoryCardAccountRangeSource(
    private val store: CardAccountRangeStore
) : CardAccountRangeSource {
    override suspend fun getAccountRange(
        cardNumber: String
    ): CardMetadata.AccountRange? {
        return cardNumber
            .take(BIN_LENGTH)
            .takeIf {
                it.length == BIN_LENGTH
            }?.let { bin ->
                store.get(bin)
                    .firstOrNull { (binRange) ->
                        binRange.matches(cardNumber)
                    }
            }
    }
}
