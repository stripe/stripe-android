package com.stripe.android.cards

import com.stripe.android.model.CardMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class InMemoryCardAccountRangeSource(
    private val store: CardAccountRangeStore
) : CardAccountRangeSource {
    override val loading: Flow<Boolean> = flowOf(false)

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
