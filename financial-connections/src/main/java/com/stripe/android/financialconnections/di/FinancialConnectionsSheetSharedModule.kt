package com.stripe.android.financialconnections.di

import android.app.Application
import androidx.core.os.LocaleListCompat
import com.stripe.android.core.ApiVersion
import com.stripe.android.core.Logger
import com.stripe.android.core.frauddetection.FraudDetectionDataRepository
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.networking.AnalyticsRequestV2Executor
import com.stripe.android.core.networking.AnalyticsRequestV2Storage
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.core.networking.DefaultAnalyticsRequestV2Executor
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.NetworkTypeDetector
import com.stripe.android.core.networking.RealAnalyticsRequestV2Storage
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.utils.ContextUtils.packageInfo
import com.stripe.android.core.utils.IsWorkManagerAvailable
import com.stripe.android.core.utils.RealIsWorkManagerAvailable
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.analytics.DefaultFinancialConnectionsEventReporter
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTrackerImpl
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEventReporter
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.IsLinkWithStripe
import com.stripe.android.financialconnections.domain.IsNetworkingRelinkSession
import com.stripe.android.financialconnections.domain.RealIsLinkWithStripe
import com.stripe.android.financialconnections.domain.RealIsNetworkingRelinkSession
import com.stripe.android.financialconnections.features.common.enableWorkManager
import com.stripe.android.financialconnections.repository.ConsumerSessionProvider
import com.stripe.android.financialconnections.repository.ConsumerSessionRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepositoryImpl
import com.stripe.android.financialconnections.repository.RealConsumerSessionRepository
import com.stripe.android.financialconnections.utils.DefaultFraudDetectionDataRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import java.util.Locale
import javax.inject.Named
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
internal interface FinancialConnectionsSheetSharedModule {

    @Binds
    @ActivityRetainedScope
    fun bindsAnalyticsRequestV2Storage(impl: RealAnalyticsRequestV2Storage): AnalyticsRequestV2Storage

    @Binds
    @ActivityRetainedScope
    fun bindsAnalyticsRequestV2Executor(impl: DefaultAnalyticsRequestV2Executor): AnalyticsRequestV2Executor

    @Binds
    @ActivityRetainedScope
    fun bindsConsumerSessionRepository(impl: RealConsumerSessionRepository): ConsumerSessionRepository

    @Binds
    @ActivityRetainedScope
    fun bindsConsumerSessionProvider(impl: RealConsumerSessionRepository): ConsumerSessionProvider

    @Binds
    fun bindsIsLinkWithStripe(impl: RealIsLinkWithStripe): IsLinkWithStripe

    @Binds
    fun bindsIsNetworkingRelinkSession(impl: RealIsNetworkingRelinkSession): IsNetworkingRelinkSession

    companion object {

        @Provides
        @ActivityRetainedScope
        @IOContext
        fun provideWorkContext(): CoroutineContext = Dispatchers.IO

        @Provides
        @ActivityRetainedScope
        @UIContext
        fun provideUIContext(): CoroutineContext = Dispatchers.Main

        @Provides
        @ActivityRetainedScope
        internal fun provideLogger(@Named(ENABLE_LOGGING) enableLogging: Boolean) =
            Logger.getInstance(enableLogging)

        @Provides
        @ActivityRetainedScope
        internal fun provideLocale() =
            LocaleListCompat.getAdjustedDefault().takeUnless { it.isEmpty }?.get(0)

        @Provides
        @ActivityRetainedScope
        internal fun providesApiOptions(
            @Named(PUBLISHABLE_KEY) publishableKey: String,
            @Named(STRIPE_ACCOUNT_ID) stripeAccountId: String?
        ): ApiRequest.Options = ApiRequest.Options(
            apiKey = publishableKey,
            stripeAccount = stripeAccountId
        )

        @Provides
        @ActivityRetainedScope
        internal fun providesJson(): Json = Json {
            coerceInputValues = true
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        @Provides
        @ActivityRetainedScope
        fun provideStripeNetworkClient(
            @IOContext context: CoroutineContext,
            logger: Logger
        ): StripeNetworkClient = DefaultStripeNetworkClient(
            workContext = context,
            logger = logger
        )

        @Provides
        @ActivityRetainedScope
        fun providesAnalyticsTracker(
            context: Application,
            getOrFetchSync: GetOrFetchSync,
            locale: Locale?,
            configuration: FinancialConnectionsSheetConfiguration,
            requestExecutor: AnalyticsRequestV2Executor,
        ): FinancialConnectionsAnalyticsTracker = FinancialConnectionsAnalyticsTrackerImpl(
            context = context,
            configuration = configuration,
            getOrFetchSync = getOrFetchSync,
            locale = locale ?: Locale.getDefault(),
            requestExecutor = requestExecutor,
        )

        @Provides
        @ActivityRetainedScope
        fun providesApiRequestFactory(
            apiVersion: ApiVersion
        ): ApiRequest.Factory = ApiRequest.Factory(
            apiVersion = apiVersion.code
        )

        @Provides
        @ActivityRetainedScope
        fun provideConnectionsRepository(
            repository: FinancialConnectionsRepositoryImpl
        ): FinancialConnectionsRepository = repository

        @Provides
        @ActivityRetainedScope
        fun provideEventReporter(
            defaultFinancialConnectionsEventReporter: DefaultFinancialConnectionsEventReporter
        ): FinancialConnectionsEventReporter = defaultFinancialConnectionsEventReporter

        @Provides
        @ActivityRetainedScope
        internal fun providesAnalyticsRequestExecutor(
            executor: DefaultAnalyticsRequestExecutor
        ): AnalyticsRequestExecutor = executor

        @Provides
        @ActivityRetainedScope
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

        @Provides
        @ActivityRetainedScope
        internal fun providesIsWorkManagerAvailable(
            getOrFetchSync: GetOrFetchSync,
        ): IsWorkManagerAvailable {
            return RealIsWorkManagerAvailable(
                isEnabledForMerchant = { getOrFetchSync().manifest.enableWorkManager() },
            )
        }

        @Provides
        @ActivityRetainedScope
        internal fun providesIoDispatcher(): CoroutineDispatcher {
            return Dispatchers.IO
        }

        @Provides
        internal fun provideFraudDetectionDataRepository(
            application: Application,
        ): FraudDetectionDataRepository {
            return DefaultFraudDetectionDataRepository(application)
        }
    }
}
