package com.stripe.android.cards

import com.stripe.android.model.AccountRange
import kotlinx.coroutines.flow.Flow

internal interface CardAccountRangeRepository {
    suspend fun getAccountRange(
        cardNumber: com.stripe.android.ui.core.elements.CardNumber.Unvalidated
    ): AccountRange?

    /**
     * Flow that represents whether any of the [CardAccountRangeSource] instances are loading.
     */
    val loading: Flow<Boolean>

    interface Factory {
        fun create(): CardAccountRangeRepository
    }
}
