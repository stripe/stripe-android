package com.stripe.android.payments.core.injection

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.Logger
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * A [Module] to provide [StripeRepository] and its corresponding dependencies.
 * [Context], [ENABLE_LOGGING], [PUBLISHABLE_KEY], [PRODUCT_USAGE] and [IOContext] need to be
 * provided elsewhere to use this module.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
abstract class StripeRepositoryModule {
    @Binds
    internal abstract fun bindsAnalyticsRequestExecutor(
        default: DefaultAnalyticsRequestExecutor
    ): AnalyticsRequestExecutor

    companion object {
        @Provides
        @Singleton
        internal fun provideStripeRepository(
            appContext: Context,
            @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
            @IOContext workContext: CoroutineContext,
            @Named(PRODUCT_USAGE) productUsageTokens: Set<String>,
            analyticsRequestFactory: AnalyticsRequestFactory,
            analyticsRequestExecutor: AnalyticsRequestExecutor,
            logger: Logger
        ): StripeRepository = StripeApiRepository(
            appContext,
            publishableKeyProvider,
            logger = logger,
            workContext = workContext,
            productUsageTokens = productUsageTokens,
            analyticsRequestFactory = analyticsRequestFactory,
            analyticsRequestExecutor = analyticsRequestExecutor
        )
    }
}
