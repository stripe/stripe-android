package com.stripe.android.core.injection

import androidx.annotation.RestrictTo
import com.stripe.android.core.networking.ExponentialBackoffRetryDelaySupplier
import com.stripe.android.core.networking.LinearRetryDelaySupplier
import com.stripe.android.core.networking.RetryDelaySupplier
import dagger.Binds
import dagger.Module
import javax.inject.Named

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
interface RetryDelayModule {
    @Binds
    fun bindsDefaultRetryDelaySupplier(
        retryDelaySupplier: ExponentialBackoffRetryDelaySupplier
    ): RetryDelaySupplier

    @Binds
    @Named(LINEAR_DELAY)
    fun bindsLinearRetryDelaySupplier(
        retryDelaySupplier: LinearRetryDelaySupplier
    ): RetryDelaySupplier

    @Binds
    @Named(EXPONENTIAL_BACKOFF_DELAY)
    fun bindsExponentialBackoffRetryDelaySupplier(
        retryDelaySupplier: ExponentialBackoffRetryDelaySupplier
    ): RetryDelaySupplier
}
