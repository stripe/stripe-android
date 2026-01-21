package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import com.stripe.android.cards.CardAccountRangeService
import com.stripe.android.cards.CardNumber
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.AccountRange
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

internal object NullUiDefinitionFactoryHelper {
    val nullEmbeddedUiDefinitionFactory = UiDefinitionFactory.Arguments.Factory.Default(
        cardAccountRangeServiceFactory = NullCardAccountRangeServiceFactory,
        linkConfigurationCoordinator = null,
        linkInlineHandler = null,
        autocompleteAddressInteractorFactory = null,
        onLinkInlineSignupStateChanged = {
            throw IllegalStateException("Not possible.")
        },
    )
}

private object NullCardAccountRangeServiceFactory : CardAccountRangeService.Factory {
    override fun create(
        cardBrandFilter: CardBrandFilter,
        cardFundingFilter: CardFundingFilter,
        accountRangeResultListener: CardAccountRangeService.AccountRangeResultListener?,
    ): CardAccountRangeService {
        return NullCardAccountRangeService
    }

    private object NullCardAccountRangeService : CardAccountRangeService {
        override val isLoading: StateFlow<Boolean> = stateFlowOf(false)
        override val accountRangesStateFlow: StateFlow<CardAccountRangeService.AccountRangesState> =
            stateFlowOf(CardAccountRangeService.AccountRangesState.Success(emptyList(), emptyList()))
        override val accountRangeResultFlow: Flow<CardAccountRangeService.AccountRangesResult> = flowOf()

        override fun onCardNumberChanged(cardNumber: CardNumber.Unvalidated, isCbcEligible: Boolean) = Unit

        override fun queryAccountRangeRepository(cardNumber: CardNumber.Unvalidated) = Unit

        override fun cancelAccountRangeRepositoryJob() = Unit

        override fun updateAccountRangesResult(accountRanges: List<AccountRange>) = Unit
    }
}
