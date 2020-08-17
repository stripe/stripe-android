package com.stripe.android.cards

import com.stripe.android.model.CardMetadata

internal class InMemoryCardAccountRangeSource(
    private val store: CardAccountRangeStore
) : CardAccountRangeSource {
    override suspend fun getAccountRange(
        cardNumber: String
    ): CardMetadata.AccountRange? {
        return Bin.create(cardNumber)?.let { (bin) ->
            store.get(bin)
                .firstOrNull { (binRange) ->
                    binRange.matches(cardNumber)
                }
        }
    }
}
