package com.stripe.android.utils

import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.CardNumber
import com.stripe.android.model.AccountRange
import com.stripe.android.networking.StripeRepository
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

object NullCardAccountRangeRepositoryFactory : CardAccountRangeRepository.Factory {
    override fun create(): CardAccountRangeRepository {
        return NullCardAccountRangeRepository
    }

    override fun createWithStripeRepository(
        stripeRepository: StripeRepository,
        publishableKey: String
    ): CardAccountRangeRepository {
        return NullCardAccountRangeRepository
    }

    private object NullCardAccountRangeRepository : CardAccountRangeRepository {
        override suspend fun getAccountRange(cardNumber: CardNumber.Unvalidated): AccountRange? {
            return null
        }

        override suspend fun getAccountRanges(cardNumber: CardNumber.Unvalidated): List<AccountRange>? {
            return null
        }

        override val loading: StateFlow<Boolean> = stateFlowOf(false)
    }
}
