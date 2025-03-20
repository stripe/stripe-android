package com.stripe.android.financialconnections.di

import android.app.Application
import com.stripe.android.core.ApiVersion
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.financialconnections.BuildConfig
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
internal object FinancialConnectionsSheetConfigurationModule {

    @Provides
    @Named(PUBLISHABLE_KEY)
    @ActivityRetainedScope
    fun providesPublishableKey(
        configuration: FinancialConnectionsSheetConfiguration
    ): String = configuration.publishableKey

    @Provides
    @Named(STRIPE_ACCOUNT_ID)
    @ActivityRetainedScope
    fun providesStripeAccountId(
        configuration: FinancialConnectionsSheetConfiguration
    ): String? = configuration.stripeAccountId

    @Provides
    @Named(ENABLE_LOGGING)
    @ActivityRetainedScope
    fun providesEnableLogging(): Boolean = BuildConfig.DEBUG

    @Provides
    @ActivityRetainedScope
    @Named(APPLICATION_ID)
    fun providesApplicationId(
        application: Application
    ): String = application.packageName

    @Provides
    @ActivityRetainedScope
    fun providesApiVersion(): ApiVersion = ApiVersion(
        betas = setOf("financial_connections_client_api_beta=v1")
    )
}
