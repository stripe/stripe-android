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
    @Named(DEFAULT_ACCOUNT_RANGE_SERVICE_FACTORY)
    @Provides
    fun providesCardAccountRangeServiceFactory(
        @Named(DEFAULT_ACCOUNT_RANGE_REPO_FACTORY) cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
        @UIContext uiContext: CoroutineContext,
        @IOContext workContext: CoroutineContext,
    ): CardAccountRangeService.Factory {
        return DefaultCardAccountRangeServiceFactory(
            cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
            uiContext = uiContext,
            workContext = workContext,
        )
    }

    @Named(FUNDING_ACCOUNT_RANGE_SERVICE_FACTORY)
    @Provides
    fun providesCardAccountRangeServiceFactoryForFunding(
        @Named(FUNDING_ACCOUNT_RANGE_REPO_FACTORY) cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val DEFAULT_ACCOUNT_RANGE_REPO_FACTORY = "DEFAULT_ACCOUNT_RANGE_REPO_FACTORY"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val DEFAULT_ACCOUNT_RANGE_SERVICE_FACTORY = "DEFAULT_ACCOUNT_RANGE_SERVICE_FACTORY"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val FUNDING_ACCOUNT_RANGE_REPO_FACTORY = "FUNDING_ACCOUNT_RANGE_REPO_FACTORY"

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
const val FUNDING_ACCOUNT_RANGE_SERVICE_FACTORY = "FUNDING_ACCOUNT_RANGE_SERVICE_FACTORY"
