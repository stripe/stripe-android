package com.stripe.android.cards

import androidx.annotation.RestrictTo
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.UIContext
import dagger.Module
import dagger.Provides
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
object CardAccountRangeServiceModule {
    @Named(DEFAULT_ACCOUNT_RANGE_REPO)
    @Provides
    fun providesCardAccountRangeServiceFactory(
        cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
        @UIContext uiContext: CoroutineContext,
        @IOContext workContext: CoroutineContext,
    ): CardAccountRangeService.Factory {
        return DefaultCardAccountRangeServiceFactory(
            cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
            uiContext = uiContext,
            workContext = workContext,
        )
    }

    @Named(FUNDING_ACCOUNT_RANGE_REPO)
    @Provides
    fun providesCardAccountRangeServiceFactoryForFunding(
        cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
        @UIContext uiContext: CoroutineContext,
        @IOContext workContext: CoroutineContext,
    ): CardAccountRangeService.Factory {
        return FundingCardAccountRangeServiceFactory(
            cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
            uiContext = uiContext,
            workContext = workContext,
        )
    }
}

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val DEFAULT_ACCOUNT_RANGE_REPO = "DEFAULT_ACCOUNT_RANGE_REPO"

@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val FUNDING_ACCOUNT_RANGE_REPO = "FUNDING_ACCOUNT_RANGE_REPO"
