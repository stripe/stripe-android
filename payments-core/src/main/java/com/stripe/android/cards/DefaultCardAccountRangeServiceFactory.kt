package com.stripe.android.cards

import android.util.Log
import androidx.annotation.RestrictTo
import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

/**
 * A factory for creating [CardAccountRangeService] instances.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultCardAccountRangeServiceFactory(
    private val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    private val uiContext: CoroutineContext = Dispatchers.Main,
    private val workContext: CoroutineContext = Dispatchers.IO,
    private val staticCardAccountRanges: StaticCardAccountRanges = DefaultStaticCardAccountRanges(),
    private val coroutineScope: CoroutineScope = CoroutineScope(uiContext),
) : CardAccountRangeService.Factory {

    override fun create(
        cardBrandFilter: CardBrandFilter,
        cardFundingFilter: CardFundingFilter,
        accountRangeResultListener: CardAccountRangeService.AccountRangeResultListener?,
    ): CardAccountRangeService {
        return DefaultCardAccountRangeService(
            cardAccountRangeRepository = cardAccountRangeRepositoryFactory.create(),
            uiContext = uiContext,
            workContext = workContext,
            staticCardAccountRanges = staticCardAccountRanges,
            cardBrandFilter = cardBrandFilter,
            cardFundingFilter = cardFundingFilter,
            accountRangeResultListener = accountRangeResultListener,
            coroutineScope = coroutineScope
        )
    }
}

/**
 * A factory for creating [CardAccountRangeService] instances for card funding.
 *
 * This factory creates separate [FundingCardAccountRangeServiceFactory] instances with their own
 * [CardAccountRangeRepository], which helps prevent pollution of shared BIN data when
 * different features (like funding filtering) use the service.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FundingCardAccountRangeServiceFactory(
    private val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    private val uiContext: CoroutineContext = Dispatchers.Main,
    private val workContext: CoroutineContext = Dispatchers.IO,
    private val staticCardAccountRanges: StaticCardAccountRanges = DefaultStaticCardAccountRanges(),
    private val coroutineScope: CoroutineScope = CoroutineScope(uiContext),
) : CardAccountRangeService.Factory {

    override fun create(
        cardBrandFilter: CardBrandFilter,
        cardFundingFilter: CardFundingFilter,
        accountRangeResultListener: CardAccountRangeService.AccountRangeResultListener?,
    ): CardAccountRangeService {
        Log.d("CardAccountRange", "FundingFactory.create() called, factory=$this")
        return DefaultCardAccountRangeService(
            cardAccountRangeRepository = cardAccountRangeRepositoryFactory.create(),
            uiContext = uiContext,
            workContext = workContext,
            staticCardAccountRanges = staticCardAccountRanges,
            cardBrandFilter = cardBrandFilter,
            cardFundingFilter = cardFundingFilter,
            accountRangeResultListener = accountRangeResultListener,
            coroutineScope = coroutineScope,
        )
    }
}
