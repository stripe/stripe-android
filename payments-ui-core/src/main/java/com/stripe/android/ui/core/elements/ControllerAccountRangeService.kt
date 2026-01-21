package com.stripe.android.ui.core.elements

import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.DefaultCardFundingFilter
import com.stripe.android.cards.CardAccountRangeService
import com.stripe.android.cards.CardNumber
import com.stripe.android.model.AccountRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class ControllerAccountRangeService(
    cardAccountRangeServiceFactory: CardAccountRangeService.Factory,
    private val cardBrandFilter: CardBrandFilter = DefaultCardBrandFilter,
    private val cardFundingFilter: CardFundingFilter = DefaultCardFundingFilter,
    coroutineScope: CoroutineScope,
    private val defaultCardAccountRangeService: CardAccountRangeService = cardAccountRangeServiceFactory.create(
        cardBrandFilter = cardBrandFilter,
        cardFundingFilter = DefaultCardFundingFilter,
    ),
    private val fundingCardAccountRangeService: CardAccountRangeService = cardAccountRangeServiceFactory.create(
        cardBrandFilter = cardBrandFilter,
        cardFundingFilter = cardFundingFilter,
    ),
) : CardAccountRangeService by defaultCardAccountRangeService {

    val fundingAccountRanges: StateFlow<List<AccountRange>> = fundingCardAccountRangeService
        .accountRangesStateFlow
        .filterIsInstance<CardAccountRangeService.AccountRangesState.Success>()
        .map { it.ranges }
        .stateIn(
            scope = coroutineScope,
            initialValue = emptyList(),
            started = SharingStarted.Eagerly
        )

    override fun onCardNumberChanged(cardNumber: CardNumber.Unvalidated, isCbcEligible: Boolean) {
        defaultCardAccountRangeService.onCardNumberChanged(cardNumber, isCbcEligible)
        fundingCardAccountRangeService.onCardNumberChanged(cardNumber, isCbcEligible)
    }
}
