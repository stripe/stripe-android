package com.stripe.android.cards

import com.stripe.android.model.CardMetadata
import kotlinx.coroutines.flow.Flow

internal interface CardAccountRangeRepository {
    suspend fun getAccountRange(
        cardNumber: CardNumber.Unvalidated
    ): CardMetadata.AccountRange?

    /**
     * Flow that represents whether any of the [CardAccountRangeSource] instances are loading.
     */
    val loading: Flow<Boolean>

    interface Factory {
        fun create(): CardAccountRangeRepository
    }
}
