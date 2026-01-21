package com.stripe.android.cards

import androidx.annotation.RestrictTo
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.UIContext
import dagger.Module
import dagger.Provides
import kotlin.coroutines.CoroutineContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
object CardAccountRangeServiceModule {
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
}
