package com.stripe.android.cards

import androidx.annotation.RestrictTo
import com.stripe.android.model.AccountRange
import kotlinx.coroutines.flow.Flow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CardAccountRangeRepository {
    suspend fun getAccountRange(
        cardNumber: CardNumber.Unvalidated
    ): AccountRange?

    /**
     * Flow that represents whether any of the [CardAccountRangeSource] instances are loading.
     */
    val loading: Flow<Boolean>

    interface Factory {
        fun create(): CardAccountRangeRepository
    }
}
