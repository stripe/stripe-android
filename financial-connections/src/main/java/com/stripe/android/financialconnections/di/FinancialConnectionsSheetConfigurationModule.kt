package com.stripe.android.financialconnections.di

import android.app.Application
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.financialconnections.BuildConfig
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
internal object FinancialConnectionsSheetConfigurationModule {

    @Provides
    @Named(PUBLISHABLE_KEY)
    @Singleton
    fun providesPublishableKey(
        configuration: FinancialConnectionsSheet.Configuration
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
