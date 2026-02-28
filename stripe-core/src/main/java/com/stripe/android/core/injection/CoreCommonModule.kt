package com.stripe.android.core.injection

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.core.os.LocaleListCompat
import com.stripe.android.core.DefaultIsExampleApp
import com.stripe.android.core.Logger
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
class CoreCommonModule {
    @Provides
    fun provideLogger(
        @Named(ENABLE_LOGGING) enableLogging: Boolean,
        context: Context,
    ) = Logger.getInstance(enableLogging, DefaultIsExampleApp(context)())

    @Provides
    @Singleton
    fun provideLocale() =
        LocaleListCompat.getAdjustedDefault().takeUnless { it.isEmpty }?.get(0)
}
