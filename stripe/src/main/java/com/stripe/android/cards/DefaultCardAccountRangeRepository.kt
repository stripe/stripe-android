package com.stripe.android.cards

import com.stripe.android.model.CardMetadata

internal class DefaultCardAccountRangeRepository(
    private val localCardAccountRangeSource: CardAccountRangeSource,
    private val remoteCardAccountRangeSource: CardAccountRangeSource
) : CardAccountRangeRepository {
    override suspend fun getAccountRange(
        cardNumber: String
    ): CardMetadata.AccountRange? {
        // TODO(mshafrir-stripe): implement
        return null
    }
}
