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
        val bin = cardNumber
            .take(CardAccountRangeSource.BIN_LENGTH)
            .takeIf {
                it.length == CardAccountRangeSource.BIN_LENGTH
            }

        return bin?.let {
            inMemoryCardAccountRangeSource.getAccountRange(cardNumber)
                ?: remoteCardAccountRangeSource.getAccountRange(cardNumber)
                ?: localCardAccountRangeSource.getAccountRange(cardNumber)
        }
    }
}
