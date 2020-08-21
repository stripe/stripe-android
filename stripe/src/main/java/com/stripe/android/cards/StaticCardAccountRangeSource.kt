package com.stripe.android.cards

import com.stripe.android.model.CardMetadata

/**
 * A [CardAccountRangeSource] that uses a local, static source of BIN ranges.
 */
internal class StaticCardAccountRangeSource(
    private val accountRanges: StaticCardAccountRanges = DefaultStaticCardAccountRanges()
) : CardAccountRangeSource {
    override suspend fun getAccountRange(
        cardNumber: CardNumber.Unvalidated
    ): CardMetadata.AccountRange? {
        return accountRanges.match(cardNumber)
    }
}
