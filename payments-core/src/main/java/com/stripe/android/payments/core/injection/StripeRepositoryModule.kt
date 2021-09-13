package com.stripe.android.payments.core.injection

import android.content.Context
import com.stripe.android.Logger
import com.stripe.android.networking.AnalyticsRequestExecutor
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * A [Module] to provide [StripeRepository] and its corresponding dependencies.
 * [Context], [ENABLE_LOGGING], [PUBLISHABLE_KEY], [PRODUCT_USAGE] and [IOContext] need to be
 * provided elsewhere to use this module.
 *
 * TODO: Let [PaymentCommonModule] include this module.
 */
@Module
class StripeRepositoryModule {
    @Provides
    @Singleton
    internal fun provideStripeRepository(
        appContext: Context,
        analyticsRequestFactory: AnalyticsRequestFactory,
        analyticsRequestExecutor: AnalyticsRequestExecutor,
        @IOContext workContext: CoroutineContext,
        @Named(ENABLE_LOGGING) enableLogging: Boolean,
        @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
    ): StripeRepository = StripeApiRepository(
        appContext,
        publishableKeyProvider,
        logger = Logger.getInstance(enableLogging),
        workContext = workContext,
        analyticsRequestFactory = analyticsRequestFactory,
        analyticsRequestExecutor = analyticsRequestExecutor
    )

    @Provides
    @Singleton
    internal fun provideAnalyticsRequestFactory(
        appContext: Context,
        @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
        @Named(PRODUCT_USAGE) productUsageTokens: Set<String>,
    ) = AnalyticsRequestFactory(appContext, publishableKeyProvider, productUsageTokens)

    @Provides
    @Singleton
    internal fun provideAnalyticsRequestExecutor(
        @Named(ENABLE_LOGGING) enableLogging: Boolean,
        @IOContext workContext: CoroutineContext
    ): AnalyticsRequestExecutor = DefaultAnalyticsRequestExecutor(
        Logger.getInstance(enableLogging),
        workContext
    )
}
