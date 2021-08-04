package com.stripe.android.networking

import androidx.annotation.RestrictTo
import com.stripe.android.Logger
import com.stripe.android.payments.core.injection.IOContext
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface AnalyticsRequestExecutor {
    /**
     * Execute the fire-and-forget request asynchronously.
     */
    fun executeAsync(request: AnalyticsRequest)

    @Module
    class DaggerModule {
        @Provides
        @Singleton
        fun provideAnalyticsRequestExecutor(
            logger: Logger,
            @IOContext workContext: CoroutineContext
        ): AnalyticsRequestExecutor = DefaultAnalyticsRequestExecutor(logger, workContext)
    }
}
