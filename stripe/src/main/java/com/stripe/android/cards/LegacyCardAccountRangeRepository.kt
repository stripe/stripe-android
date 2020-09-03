package com.stripe.android.cards

import com.stripe.android.model.CardMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * A [CardAccountRangeRepository] that simulates existing card account range lookup logic by only
 * using a local, static source.
 */
internal class LegacyCardAccountRangeRepository(
    private val staticCardAccountRangeSource: CardAccountRangeSource
) : CardAccountRangeRepository {
    override suspend fun getAccountRange(
        cardNumber: CardNumber.Unvalidated
    ): CardMetadata.AccountRange? {
        return cardNumber.bin?.let {
            staticCardAccountRangeSource.getAccountRange(cardNumber)
        }
    }

    override val loading: Flow<Boolean> = flowOf(false)
}
