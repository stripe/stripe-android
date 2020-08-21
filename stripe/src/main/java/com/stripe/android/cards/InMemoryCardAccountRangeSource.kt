package com.stripe.android.cards

import com.stripe.android.model.CardMetadata

internal class InMemoryCardAccountRangeSource(
    private val store: CardAccountRangeStore
) : CardAccountRangeSource {
    override suspend fun getAccountRange(
        cardNumber: CardNumber.Unvalidated
    ): CardMetadata.AccountRange? {
        return cardNumber.bin?.let { bin ->
            store.get(bin)
                .firstOrNull { (binRange) ->
                    binRange.matches(cardNumber)
                }
        }
    }
}
