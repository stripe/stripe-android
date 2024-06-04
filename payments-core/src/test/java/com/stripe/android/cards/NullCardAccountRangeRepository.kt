package com.stripe.android.cards

import com.stripe.android.model.AccountRange
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal class NullCardAccountRangeRepository : CardAccountRangeRepository {
    override suspend fun getAccountRange(
        cardNumber: CardNumber.Unvalidated
    ): AccountRange? = null

    override suspend fun getAccountRanges(
        cardNumber: CardNumber.Unvalidated
    ): List<AccountRange>? = null

    override val loading: StateFlow<Boolean> = stateFlowOf(false)
}
