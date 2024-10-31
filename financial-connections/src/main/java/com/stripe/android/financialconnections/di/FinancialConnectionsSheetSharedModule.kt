package com.stripe.android.financialconnections.di

import android.app.Application
import com.stripe.android.core.ApiVersion
import com.stripe.android.core.Logger
import com.stripe.android.core.frauddetection.FraudDetectionDataRepository
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
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
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.analytics.DefaultFinancialConnectionsEventReporter
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTrackerImpl
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEventReporter
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.IsLinkWithStripe
import com.stripe.android.financialconnections.domain.RealIsLinkWithStripe
import com.stripe.android.financialconnections.features.common.enableWorkManager
import com.stripe.android.financialconnections.repository.ConsumerSessionProvider
import com.stripe.android.financialconnections.repository.ConsumerSessionRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepositoryImpl
import com.stripe.android.financialconnections.repository.RealConsumerSessionRepository
import com.stripe.android.financialconnections.utils.DefaultFraudDetectionDataRepository
import com.stripe.attestation.IntegrityStandardRequestManager
import com.stripe.attestation.RealStandardIntegrityManagerFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
internal interface FinancialConnectionsSheetSharedModule {

    @Binds
    @Singleton
    fun bindsAnalyticsRequestV2Storage(impl: RealAnalyticsRequestV2Storage): AnalyticsRequestV2Storage

    @Binds
    @Singleton
    fun bindsAnalyticsRequestV2Executor(impl: DefaultAnalyticsRequestV2Executor): AnalyticsRequestV2Executor

    @Binds
    @Singleton
    fun bindsConsumerSessionRepository(impl: RealConsumerSessionRepository): ConsumerSessionRepository

    @Binds
    @Singleton
    fun bindsConsumerSessionProvider(impl: RealConsumerSessionRepository): ConsumerSessionProvider

    @Binds
    fun bindsIsLinkWithStripe(impl: RealIsLinkWithStripe): IsLinkWithStripe

    companion object {

        @Singleton
        @Provides
        fun providesIntegrityStandardRequestManager(
            context: Application,
            logger: Logger
        ): IntegrityStandardRequestManager = IntegrityStandardRequestManager(
            cloudProjectNumber = 527113280969, //stripe-financial-connections
            logError = { message, error -> logger.error(message, error) },
            factory = RealStandardIntegrityManagerFactory(context)
        )

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
            getOrFetchSync: GetOrFetchSync,
            locale: Locale?,
            configuration: FinancialConnectionsSheet.Configuration,
            requestExecutor: AnalyticsRequestV2Executor,
        ): FinancialConnectionsAnalyticsTracker = FinancialConnectionsAnalyticsTrackerImpl(
            context = context,
            configuration = configuration,
            getOrFetchSync = getOrFetchSync,
            locale = locale ?: Locale.getDefault(),
            requestExecutor = requestExecutor,
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

        @Provides
        @Singleton
        internal fun providesIsWorkManagerAvailable(
            getOrFetchSync: GetOrFetchSync,
        ): IsWorkManagerAvailable {
            return RealIsWorkManagerAvailable(
                isEnabledForMerchant = { getOrFetchSync().manifest.enableWorkManager() },
            )
        }

        @Provides
        @Singleton
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
