package com.stripe.android.cards

import com.stripe.android.model.AccountRange
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal class InMemoryCardAccountRangeSource(
    private val store: CardAccountRangeStore
) : CardAccountRangeSource {
    override val loading: StateFlow<Boolean> = stateFlowOf(false)

    override suspend fun getAccountRanges(
        cardNumber: CardNumber.Unvalidated
    ): List<AccountRange>? {
        return cardNumber.bin?.let { bin ->
            store.get(bin)
        }
    }
}
