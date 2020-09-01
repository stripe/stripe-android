package com.stripe.android.cards

import com.stripe.android.model.CardMetadata

internal class NullCardAccountRangeRepository : CardAccountRangeRepository {
    override suspend fun getAccountRange(
        cardNumber: CardNumber.Unvalidated
    ): CardMetadata.AccountRange? = null
}
