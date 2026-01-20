package com.stripe.android.utils

import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import com.stripe.android.cards.CardAccountRangeService
import com.stripe.android.cards.CardNumber
import com.stripe.android.model.AccountRange
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

object NullCardAccountRangeServiceFactory : CardAccountRangeService.Factory {
    override fun create(
        cardBrandFilter: CardBrandFilter,
        cardFundingFilter: CardFundingFilter,
        accountRangeResultListener: CardAccountRangeService.AccountRangeResultListener?,
        coroutineScope: CoroutineScope?
    ): CardAccountRangeService {
        return NullCardAccountRangeService
    }

    private object NullCardAccountRangeService : CardAccountRangeService {
        override val isLoading: StateFlow<Boolean> = stateFlowOf(false)
        override val accountRangesStateFlow: StateFlow<CardAccountRangeService.AccountRangesState> =
            stateFlowOf(CardAccountRangeService.AccountRangesState.Success(emptyList(), emptyList()))
        override val accountRangeResultFlow: Flow<CardAccountRangeService.AccountRangesResult> =
            flowOf()

        override fun onCardNumberChanged(
            cardNumber: CardNumber.Unvalidated,
            isCbcEligible: Boolean
        ) {
            // No-op
        }

        override fun queryAccountRangeRepository(cardNumber: CardNumber.Unvalidated) {
            // No-op
        }

        override fun cancelAccountRangeRepositoryJob() {
            // No-op
        }

        override fun updateAccountRangesResult(accountRanges: List<AccountRange>) {
            // No-op
        }
    }
}
