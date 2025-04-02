package com.stripe.android.payments.core.injection

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestV2Executor
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.core.networking.DefaultAnalyticsRequestV2Executor
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.RealAnalyticsRequestV2Storage
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.networking.StripeRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlin.coroutines.CoroutineContext

/**
 * A [Module] to provide [StripeRepository] and its corresponding dependencies.
 * [Context], [Logger], [PUBLISHABLE_KEY], [PRODUCT_USAGE] and [IOContext] need to be
 * provided elsewhere to use this module.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
abstract class StripeRepositoryModule {
    @Binds
    internal abstract fun bindsAnalyticsRequestExecutor(
        default: DefaultAnalyticsRequestExecutor
    ): AnalyticsRequestExecutor

    @Binds
    internal abstract fun bindsStripeRepository(
        stripeApiRepository: StripeApiRepository
    ): StripeRepository

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        @Provides
        fun providesAnalyticsRequestV2Executor(
            application: Context,
            @IOContext coroutineContext: CoroutineContext,
            logger: Logger
        ): AnalyticsRequestV2Executor = DefaultAnalyticsRequestV2Executor(
            application,
            networkClient = DefaultStripeNetworkClient(
                logger = logger,
                workContext = coroutineContext
            ),
            logger = logger,
            storage = RealAnalyticsRequestV2Storage(application),
            isWorkManagerAvailable = { false }
        )
    }
}
