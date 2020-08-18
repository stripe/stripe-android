package com.stripe.android.cards

import com.stripe.android.model.CardMetadata

/**
 * A [CardAccountRangeRepository] that simulates existing card account range lookup logic by only
 * using a local, static source.
 */
internal class LegacyCardAccountRangeRepository(
    private val localCardAccountRangeSource: CardAccountRangeSource
) : CardAccountRangeRepository {
    override suspend fun getAccountRange(cardNumber: String): CardMetadata.AccountRange? {
        return Bin.create(cardNumber)?.let {
            localCardAccountRangeSource.getAccountRange(cardNumber)
        }
    }
}
