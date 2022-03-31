package com.stripe.android.connections.di

import android.app.Application
import androidx.core.os.LocaleListCompat
import com.stripe.android.connections.analytics.ConnectionsEventReporter
import com.stripe.android.connections.analytics.DefaultConnectionsEventReporter
import com.stripe.android.connections.repository.ConnectionsApiRepository
import com.stripe.android.connections.repository.ConnectionsRepository
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.StripeNetworkClient
import dagger.Module
import dagger.Provides
import java.util.Locale
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module(
    includes = [ConnectionsSheetConfigurationModule::class]
)
internal object ConnectionsSheetModule {

    @Provides
    @Singleton
    fun provideStripeNetworkClient(
        @IOContext context: CoroutineContext,
        logger: Logger
    ): StripeNetworkClient = DefaultStripeNetworkClient(
        workContext = context,
        logger = logger
    )

    @Provides
    @Singleton
    fun providesApiRequestFactory(): ApiRequest.Factory = ApiRequest.Factory()

    @Provides
    @Singleton
    fun provideConnectionsRepository(
        repository: ConnectionsApiRepository
    ): ConnectionsRepository = repository

    @Provides
    @Singleton
    fun provideLocale(): Locale? =
        LocaleListCompat.getAdjustedDefault().takeUnless { it.isEmpty }?.get(0)

    @Provides
    @Singleton
    fun provideEventReporter(
        defaultConnectionsEventReporter: DefaultConnectionsEventReporter
    ): ConnectionsEventReporter = defaultConnectionsEventReporter

    @Provides
    @Singleton
    internal fun providesAnalyticsRequestExecutor(
        executor: DefaultAnalyticsRequestExecutor
    ): AnalyticsRequestExecutor = executor

    @Provides
    @Singleton
    internal fun provideAnalyticsRequestFactory(
        application: Application,
        @Named(PUBLISHABLE_KEY) publishableKey: String,
    ): AnalyticsRequestFactory = AnalyticsRequestFactory(
        packageManager = application.packageManager,
        packageName = application.packageName.orEmpty(),
        packageInfo = application.packageManager.getPackageInfo(application.packageName, 0),
        publishableKeyProvider = { publishableKey }
    )
}
