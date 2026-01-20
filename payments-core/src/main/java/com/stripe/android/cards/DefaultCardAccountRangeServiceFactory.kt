package com.stripe.android.cards

import androidx.annotation.RestrictTo
import com.stripe.android.CardBrandFilter
import com.stripe.android.CardFundingFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

/**
 * A factory for creating [CardAccountRangeService] instances.
 *
 * This factory creates separate [DefaultCardAccountRangeService] instances with their own
 * [CardAccountRangeRepository], which helps prevent pollution of shared BIN data when
 * different features (like funding filtering) use the service.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultCardAccountRangeServiceFactory @JvmOverloads constructor(
    private val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    private val uiContext: CoroutineContext = Dispatchers.Main,
    private val workContext: CoroutineContext = Dispatchers.IO,
    private val staticCardAccountRanges: StaticCardAccountRanges = DefaultStaticCardAccountRanges()
) : CardAccountRangeService.Factory {

    override fun create(
        cardBrandFilter: CardBrandFilter,
        cardFundingFilter: CardFundingFilter,
        accountRangeResultListener: CardAccountRangeService.AccountRangeResultListener?,
        coroutineScope: CoroutineScope?
    ): CardAccountRangeService {
        val scope = coroutineScope ?: CoroutineScope(uiContext)
        return DefaultCardAccountRangeService(
            cardAccountRangeRepository = cardAccountRangeRepositoryFactory.create(),
            uiContext = uiContext,
            workContext = workContext,
            staticCardAccountRanges = staticCardAccountRanges,
            cardBrandFilter = cardBrandFilter,
            cardFundingFilter = cardFundingFilter,
            accountRangeResultListener = accountRangeResultListener,
            coroutineScope = scope
        )
    }
}
