package com.stripe.android.cards

import com.stripe.android.model.CardMetadata

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
}
