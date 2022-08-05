package com.stripe.android.core.injection

import androidx.annotation.RestrictTo
import androidx.core.os.LocaleListCompat
import com.stripe.android.core.Logger
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
class CoreCommonModule {
    @Provides
    @Singleton
    fun provideLogger(@Named(ENABLE_LOGGING) enableLogging: Boolean) =
        Logger.getInstance(enableLogging)

    @Provides
    @Singleton
    fun provideLocale() =
        LocaleListCompat.getAdjustedDefault().takeUnless { it.isEmpty }?.get(0)
}
