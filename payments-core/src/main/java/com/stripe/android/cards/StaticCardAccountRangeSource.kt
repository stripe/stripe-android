package com.stripe.android.cards

import androidx.annotation.RestrictTo
import com.stripe.android.model.AccountRange
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

/**
 * A [CardAccountRangeSource] that uses a local, static source of BIN ranges.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class StaticCardAccountRangeSource(
    private val accountRanges: StaticCardAccountRanges = DefaultStaticCardAccountRanges()
) : CardAccountRangeSource {
    override val loading: StateFlow<Boolean> = stateFlowOf(false)

    override suspend fun getAccountRanges(
        cardNumber: CardNumber.Unvalidated
    ): List<AccountRange> {
        return accountRanges.filter(cardNumber)
    }
}
