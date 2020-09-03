package com.stripe.android.cards

import com.stripe.android.model.CardMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class NullCardAccountRangeRepository : CardAccountRangeRepository {
    override suspend fun getAccountRange(
        cardNumber: CardNumber.Unvalidated
    ): CardMetadata.AccountRange? = null

    override val loading: Flow<Boolean> = flowOf(false)
}
