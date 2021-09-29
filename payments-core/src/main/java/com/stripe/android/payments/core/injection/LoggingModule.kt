package com.stripe.android.payments.core.injection

import androidx.annotation.RestrictTo
import com.stripe.android.Logger
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
class LoggingModule {
    @Provides
    @Singleton
    fun provideLogger(@Named(ENABLE_LOGGING) enableLogging: Boolean) =
        Logger.getInstance(enableLogging)
}
