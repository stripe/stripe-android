package com.stripe.android.core.injection

import androidx.annotation.RestrictTo
import com.stripe.android.core.networking.ExponentialBackoffRetryDelaySupplier
import com.stripe.android.core.networking.LinearRetryDelaySupplier
import com.stripe.android.core.networking.RetryDelaySupplier
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
object RetryDelayModule {
    @Provides
    @Singleton
    fun provideDefaultBackoffRetryDelaySupplier(): RetryDelaySupplier {
        return ExponentialBackoffRetryDelaySupplier()
    }

    @Provides
    @Singleton
    @Named(LINEAR_DELAY)
    fun provideLinearRetryDelaySupplier(): RetryDelaySupplier {
        return LinearRetryDelaySupplier()
    }

    @Provides
    @Singleton
    @Named(EXPONENTIAL_BACKOFF_DELAY)
    fun provideExponentialBackoffRetryDelaySupplier(): RetryDelaySupplier {
        return ExponentialBackoffRetryDelaySupplier()
    }
}
