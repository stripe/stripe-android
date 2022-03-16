package com.stripe.android.connections.di

import android.app.Application
import android.content.Context
import com.stripe.android.connections.ConnectionsSheet
import com.stripe.android.connections.ConnectionsSheetContract
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.injection.ENABLE_LOGGING
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
internal object ConnectionsSheetConfigurationModule {

    @Provides
    @Singleton
    fun providesContext(application: Application): Context {
        return application
    }

    @Provides
    @Singleton
    fun providesConfiguration(
        args: ConnectionsSheetContract.Args
    ): ConnectionsSheet.Configuration = args.configuration

    @Provides
    @Named(PUBLISHABLE_KEY)
    @Singleton
    fun providesPublishableKey(
        configuration: ConnectionsSheet.Configuration
    ): String = configuration.publishableKey

    @Provides
    @Named(ENABLE_LOGGING)
    @Singleton
    fun providesEnableLogging(): Boolean = BuildConfig.DEBUG

    @Provides
    @Singleton
    @Named(APPLICATION_ID)
    fun providesApplicationId(
        application: Application
    ): String = application.packageName
}
