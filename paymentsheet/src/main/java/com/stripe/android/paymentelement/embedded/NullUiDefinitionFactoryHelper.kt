package com.stripe.android.paymentelement.embedded

import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.CardNumber
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.AccountRange
import com.stripe.android.networking.StripeRepository
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal object NullUiDefinitionFactoryHelper {
    val nullEmbeddedUiDefinitionFactory = UiDefinitionFactory.Arguments.Factory.Default(
        cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
        linkConfigurationCoordinator = null,
        onLinkInlineSignupStateChanged = {
            throw IllegalStateException("Not possible.")
        },
    )
}

private object NullCardAccountRangeRepositoryFactory : CardAccountRangeRepository.Factory {
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
