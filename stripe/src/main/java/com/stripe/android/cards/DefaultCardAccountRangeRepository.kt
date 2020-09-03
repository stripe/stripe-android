package com.stripe.android.cards

import com.stripe.android.model.CardMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class DefaultCardAccountRangeRepository(
    private val inMemoryCardAccountRangeSource: CardAccountRangeSource,
    private val remoteCardAccountRangeSource: CardAccountRangeSource,
    private val staticCardAccountRangeSource: CardAccountRangeSource
) : CardAccountRangeRepository {
    override suspend fun getAccountRange(
        cardNumber: CardNumber.Unvalidated
    ): CardMetadata.AccountRange? {
        return cardNumber.bin?.let {
            inMemoryCardAccountRangeSource.getAccountRange(cardNumber)
                ?: remoteCardAccountRangeSource.getAccountRange(cardNumber)
                ?: staticCardAccountRangeSource.getAccountRange(cardNumber)
        }
    }

    override val loading: Flow<Boolean> = combine(
        listOf(
            inMemoryCardAccountRangeSource.loading,
            remoteCardAccountRangeSource.loading,
            staticCardAccountRangeSource.loading
        )
    ) { loading ->
        // emit true if any of the sources are loading data
        loading.any { it }
    }
}
