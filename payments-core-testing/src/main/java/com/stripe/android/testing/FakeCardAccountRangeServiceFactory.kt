package com.stripe.android.testing

import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.cards.CardAccountRangeService
import com.stripe.android.cards.CardNumber
import com.stripe.android.cards.DefaultCardAccountRangeServiceFactory
import com.stripe.android.cards.StaticCardAccountRangeSource
import com.stripe.android.model.AccountRange
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext

/**
 * A fake implementation of [CardAccountRangeService.Factory] for testing purposes.
 *
 * This factory creates [CardAccountRangeService] instances that use a static account range source,
 * making it suitable for unit tests that don't need to make actual network calls.
 */
class FakeCardAccountRangeServiceFactory(
    private val uiContext: CoroutineContext = Dispatchers.Main,
    private val workContext: CoroutineContext = Dispatchers.IO
) : CardAccountRangeService.Factory {

    override fun create(
        cardBrandFilter: CardBrandFilter,
        cardFundingFilter: CardFundingFilter,
        accountRangeResultListener: CardAccountRangeService.AccountRangeResultListener?,
        coroutineScope: CoroutineScope?
    ): CardAccountRangeService {
        return DefaultCardAccountRangeServiceFactory(
            cardAccountRangeRepositoryFactory = FakeCardAccountRangeRepositoryFactory(),
            uiContext = uiContext,
            workContext = workContext
        ).create(
            cardBrandFilter = cardBrandFilter,
            cardFundingFilter = cardFundingFilter,
            accountRangeResultListener = accountRangeResultListener,
            coroutineScope = coroutineScope
        )
    }
}

private class FakeCardAccountRangeRepositoryFactory : CardAccountRangeRepository.Factory {
    override fun create(): CardAccountRangeRepository {
        return FakeCardAccountRangeRepository()
    }

    override fun createWithStripeRepository(
        stripeRepository: StripeRepository,
        publishableKey: String
    ): CardAccountRangeRepository {
        return FakeCardAccountRangeRepository()
    }
}

private class FakeCardAccountRangeRepository : CardAccountRangeRepository {
    private val staticCardAccountRangeSource = StaticCardAccountRangeSource()

    override suspend fun getAccountRange(
        cardNumber: CardNumber.Unvalidated
    ): AccountRange? {
        return cardNumber.bin?.let {
            staticCardAccountRangeSource.getAccountRange(cardNumber)
        }
    }

    override suspend fun getAccountRanges(
        cardNumber: CardNumber.Unvalidated
    ): List<AccountRange>? {
        return cardNumber.bin?.let {
            staticCardAccountRangeSource.getAccountRanges(cardNumber)
        }
    }

    override val loading: StateFlow<Boolean> = MutableStateFlow(false)
}
