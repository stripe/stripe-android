package com.stripe.android.financialconnections.di

import android.app.Application
import androidx.core.os.LocaleListCompat
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.utils.ContextUtils.packageInfo
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.analytics.DefaultFinancialConnectionsEventReporter
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEventReporter
import com.stripe.android.financialconnections.repository.FinancialConnectionsApiRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import dagger.Module
import dagger.Provides
import java.util.Locale
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module
internal object FinancialConnectionsSheetSharedModule {

    @Provides
    @Named(PUBLISHABLE_KEY)
    @ActivityScoped
    fun providesPublishableKey(
        configuration: FinancialConnectionsSheet.Configuration
    ): String = configuration.publishableKey

    @Provides
    @ActivityScoped
    fun providesApiRequestFactory(): ApiRequest.Factory = ApiRequest.Factory()

    @Provides
    @ActivityScoped
    fun provideConnectionsRepository(
        repository: FinancialConnectionsApiRepository
    ): FinancialConnectionsRepository = repository

    @Provides
    @ActivityScoped
    fun provideEventReporter(
        defaultFinancialConnectionsEventReporter: DefaultFinancialConnectionsEventReporter
    ): FinancialConnectionsEventReporter = defaultFinancialConnectionsEventReporter

    @Provides
    @ActivityScoped
    internal fun providesAnalyticsRequestExecutor(
        executor: DefaultAnalyticsRequestExecutor
    ): AnalyticsRequestExecutor = executor

    @Provides
    @ActivityScoped
    internal fun provideAnalyticsRequestFactory(
        application: Application,
        @Named(PUBLISHABLE_KEY) publishableKey: String
    ): AnalyticsRequestFactory = AnalyticsRequestFactory(
        packageManager = application.packageManager,
        packageName = application.packageName.orEmpty(),
        packageInfo = application.packageInfo,
        publishableKeyProvider = { publishableKey }
    )
}
