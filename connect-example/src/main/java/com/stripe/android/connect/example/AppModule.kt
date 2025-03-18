package com.stripe.android.connect.example

import com.stripe.android.core.Logger
import com.stripe.android.core.injection.ENABLE_LOGGING
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    @Named(ENABLE_LOGGING)
    fun provideEnableLogging(): Boolean = BuildConfig.DEBUG

    @Singleton
    @Provides
    fun provideLogger(@Named(ENABLE_LOGGING) enabled: Boolean): Logger {
        return Logger.getInstance(enableLogging = enabled)
    }
}
