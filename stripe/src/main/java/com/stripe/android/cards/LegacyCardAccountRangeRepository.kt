package com.stripe.android.cards

import com.stripe.android.model.CardMetadata

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
}
