package com.stripe.android.cards

import com.stripe.android.model.AccountRange
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.StateFlow

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

    override val loading: StateFlow<Boolean> = combineAsStateFlow(
        inMemorySource.loading,
        remoteSource.loading,
        staticSource.loading
    ) { loading1, loading2, loading3 ->
        loading1 || loading2 || loading3
    }
}
