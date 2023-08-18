package com.stripe.android.financialconnections.di

import android.app.Application
import com.stripe.android.core.ApiVersion
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.NetworkTypeDetector
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.utils.ContextUtils.packageInfo
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.analytics.DefaultFinancialConnectionsEventReporter
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTrackerImpl
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEventReporter
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepositoryImpl
import dagger.Module
import dagger.Provides
import kotlinx.serialization.json.Json
import java.util.Locale
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Dependencies shared between activities should live here.
 *
 * Components that depend on user-provided configuration should be here so that they can be
 * recovered after a process kill. The typical component recreation flow would be:
 *
 * 1. user-provided config field is saved on[android.app.Activity.onSaveInstanceState]
 * 2. field is recovered in [android.app.Activity.onRestoreInstanceState]
 * 3. field is passed as a bound instance to the component using this module.
 * 4. component gets recreated.
 *
 * Each activity implementing this module should be responsible for recovering from process kills
 * saving and restoring user-provided configuration dependencies.
 */
@Module(
    includes = [FinancialConnectionsSheetConfigurationModule::class]
)
internal object FinancialConnectionsSheetSharedModule {

    @Provides
    @Singleton
    internal fun providesApiOptions(
        @Named(PUBLISHABLE_KEY) publishableKey: String,
        @Named(STRIPE_ACCOUNT_ID) stripeAccountId: String?
    ): ApiRequest.Options = ApiRequest.Options(
        apiKey = publishableKey,
        stripeAccount = stripeAccountId
    )

    @Provides
    @Singleton
    internal fun providesJson(): Json = Json {
        coerceInputValues = true
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideStripeNetworkClient(
        @IOContext context: CoroutineContext,
        logger: Logger
    ): StripeNetworkClient = DefaultStripeNetworkClient(
        workContext = context,
        logger = logger
    )

    @Singleton
    @Provides
    fun providesAnalyticsTracker(
        context: Application,
        logger: Logger,
        getManifest: GetManifest,
        locale: Locale?,
        configuration: FinancialConnectionsSheet.Configuration,
        stripeNetworkClient: StripeNetworkClient
    ): FinancialConnectionsAnalyticsTracker = FinancialConnectionsAnalyticsTrackerImpl(
        context = context,
        configuration = configuration,
        getManifest = getManifest,
        logger = logger,
        locale = locale ?: Locale.getDefault(),
        stripeNetworkClient = stripeNetworkClient
    )

    @Provides
    @Singleton
    fun providesApiRequestFactory(
        apiVersion: ApiVersion
    ): ApiRequest.Factory = ApiRequest.Factory(
        apiVersion = apiVersion.code
    )

    @Provides
    @Singleton
    fun provideConnectionsRepository(
        repository: FinancialConnectionsRepositoryImpl
    ): FinancialConnectionsRepository = repository

    @Provides
    @Singleton
    fun provideEventReporter(
        defaultFinancialConnectionsEventReporter: DefaultFinancialConnectionsEventReporter
    ): FinancialConnectionsEventReporter = defaultFinancialConnectionsEventReporter

    @Provides
    @Singleton
    internal fun providesAnalyticsRequestExecutor(
        executor: DefaultAnalyticsRequestExecutor
    ): AnalyticsRequestExecutor = executor

    @Provides
    @Singleton
    internal fun provideAnalyticsRequestFactory(
        application: Application,
        @Named(PUBLISHABLE_KEY) publishableKey: String
    ): AnalyticsRequestFactory = AnalyticsRequestFactory(
        packageManager = application.packageManager,
        packageName = application.packageName.orEmpty(),
        packageInfo = application.packageInfo,
        publishableKeyProvider = { publishableKey },
        networkTypeProvider = NetworkTypeDetector(application)::invoke,
    )
}
