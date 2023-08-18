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
            val range = if (store.contains(bin)) {
                inMemorySource.getAccountRange(cardNumber)
            } else {
                remoteSource.getAccountRange(cardNumber)
            }

            range ?: staticSource.getAccountRange(cardNumber)
        }
    }

    override suspend fun getAccountRanges(
        cardNumber: CardNumber.Unvalidated
    ): List<AccountRange>? {
        return cardNumber.bin?.let { bin ->
            val ranges = if (store.contains(bin)) {
                inMemorySource.getAccountRanges(cardNumber)
            } else {
                remoteSource.getAccountRanges(cardNumber)
            }

            ranges?.takeIf { it.isNotEmpty() } ?: staticSource.getAccountRanges(cardNumber)
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
