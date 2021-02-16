package com.stripe.android.cards

import com.stripe.android.model.AccountRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * A [CardAccountRangeSource] that uses a local, static source of BIN ranges.
 */
internal class StaticCardAccountRangeSource(
    private val accountRanges: StaticCardAccountRanges = DefaultStaticCardAccountRanges()
) : CardAccountRangeSource {
    override val loading: Flow<Boolean> = flowOf(false)

    override suspend fun getAccountRange(
        cardNumber: CardNumber.Unvalidated
    ): AccountRange? {
        return accountRanges.first(cardNumber)
    }
}
