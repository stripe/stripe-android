package com.stripe.android.cards

import com.stripe.android.model.CardMetadata

/**
 * A [CardAccountRangeSource] that uses a local, static source of BIN ranges.
 */
internal class LocalCardAccountRangeSource : CardAccountRangeSource {
    override suspend fun getAccountRange(
        cardNumber: CardNumber.Unvalidated
    ): CardMetadata.AccountRange? {
        return StaticAccountRanges.ACCOUNTS
            .firstOrNull {
                it.binRange.matches(cardNumber)
            }
    }
}
