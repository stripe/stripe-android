package com.stripe.android.financialconnections.di

import android.app.Application
import androidx.core.os.LocaleListCompat
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.LoggingModule
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.financialconnections.BuildConfig
import dagger.Module
import dagger.Provides
import java.util.Locale
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module(
    subcomponents = [
        FinancialConnectionsSheetActivitySubcomponent::class,
        FinancialConnectionsSheetNativeActivitySubcomponent::class
    ],
    includes = [
        LoggingModule::class,
        CoroutineContextModule::class
    ]
)
internal class FinancialConnectionsApplicationModule {
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

    @Provides
    @Singleton
    fun providesApiRequestFactory(): ApiRequest.Factory = ApiRequest.Factory()

    @Provides
    @Singleton
    fun provideLocale(): Locale? =
        LocaleListCompat.getAdjustedDefault().takeUnless { it.isEmpty }?.get(0)

    @Provides
    @Singleton
    fun provideStripeNetworkClient(
        @IOContext context: CoroutineContext,
        logger: Logger
    ): StripeNetworkClient = DefaultStripeNetworkClient(
        workContext = context,
        logger = logger
    )
}
