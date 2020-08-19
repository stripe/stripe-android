package com.stripe.android.cards

import com.stripe.android.model.CardMetadata

internal class DefaultCardAccountRangeRepository(
    private val inMemoryCardAccountRangeSource: CardAccountRangeSource,
    private val localCardAccountRangeSource: CardAccountRangeSource,
    private val remoteCardAccountRangeSource: CardAccountRangeSource
) : CardAccountRangeRepository {
    override suspend fun getAccountRange(
        cardNumber: String
    ): CardMetadata.AccountRange? {
        return Bin.create(cardNumber)?.let {
            inMemoryCardAccountRangeSource.getAccountRange(cardNumber)
                ?: remoteCardAccountRangeSource.getAccountRange(cardNumber)
                ?: localCardAccountRangeSource.getAccountRange(cardNumber)
        }
    }
}
