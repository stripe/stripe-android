package com.stripe.android.cards

import com.stripe.android.model.AccountRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class DefaultCardAccountRangeRepository(
    private val inMemorySource: CardAccountRangeSource,
    private val remoteSource: CardAccountRangeSource,
    private val staticSource: CardAccountRangeSource,
    private val store: CardAccountRangeStore
) : CardAccountRangeRepository {
    override suspend fun getAccountRange(
        cardNumber: CardNumber.Unvalidated
    ): AccountRange? {
        return cardNumber.bin?.let { bin ->
            if (store.contains(bin)) {
                inMemorySource.getAccountRange(cardNumber)
            } else {
                remoteSource.getAccountRange(cardNumber)
            } ?: staticSource.getAccountRange(cardNumber)
        }
    }

    override val loading: Flow<Boolean> = combine(
        listOf(
            inMemorySource.loading,
            remoteSource.loading,
            staticSource.loading
        )
    ) { loading ->
        // emit true if any of the sources are loading data
        loading.any { it }
    }
}
