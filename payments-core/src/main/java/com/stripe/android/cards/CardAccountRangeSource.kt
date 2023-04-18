package com.stripe.android.cards

import com.stripe.android.model.AccountRange
import kotlinx.coroutines.flow.Flow

internal interface CardAccountRangeSource {
    suspend fun getAccountRange(
        cardNumber: CardNumber.Unvalidated
    ): AccountRange? {
        return getAccountRanges(cardNumber)?.firstOrNull { (binRange) ->
            binRange.matches(cardNumber)
        }
    }

    suspend fun getAccountRanges(
        cardNumber: CardNumber.Unvalidated
    ): List<AccountRange>?

    val loading: Flow<Boolean>
}
