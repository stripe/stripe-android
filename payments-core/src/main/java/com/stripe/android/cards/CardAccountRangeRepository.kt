package com.stripe.android.cards

import androidx.annotation.RestrictTo
import com.stripe.android.model.AccountRange
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.flow.Flow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CardAccountRangeRepository {
    suspend fun getAccountRange(
        cardNumber: CardNumber.Unvalidated
    ): AccountRange?

    suspend fun getAccountRanges(
        cardNumber: CardNumber.Unvalidated
    ): List<AccountRange>?

    /**
     * Flow that represents whether any of the [CardAccountRangeSource] instances are loading.
     */
    val loading: Flow<Boolean>

    interface Factory {
        fun create(): CardAccountRangeRepository
        fun createWithStripeRepository(
            stripeRepository: StripeRepository,
            publishableKey: String
        ): CardAccountRangeRepository
    }
}
