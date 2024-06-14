package com.stripe.android.financialconnections.di

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.ApiVersion
import com.stripe.android.core.Logger
import com.stripe.android.core.error.ErrorReporter
import com.stripe.android.core.error.SentryErrorReporter
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.financialconnections.domain.HandleError
import com.stripe.android.financialconnections.domain.RealHandleError
import com.stripe.android.financialconnections.error.FinancialConnectionsSentryConfig
import com.stripe.android.financialconnections.features.accountupdate.PresentAccountUpdateRequiredSheet
import com.stripe.android.financialconnections.features.accountupdate.RealPresentAccountUpdateRequiredSheet
import com.stripe.android.financialconnections.features.notice.PresentNoticeSheet
import com.stripe.android.financialconnections.features.notice.RealPresentNoticeSheet
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.NavigationManagerImpl
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsInstitutionsRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import com.stripe.android.financialconnections.repository.api.FinancialConnectionsConsumersApiService
import com.stripe.android.repository.ConsumersApiService
import com.stripe.android.repository.ConsumersApiServiceImpl
import com.stripe.android.uicore.image.StripeImageLoader
import dagger.Binds
import dagger.Module
import dagger.Provides
import java.util.Locale
import javax.inject.Named
import javax.inject.Singleton

@Module
internal interface FinancialConnectionsSheetNativeModule {

    @Binds
    fun bindsPresentNoticeSheet(impl: RealPresentNoticeSheet): PresentNoticeSheet

    @Binds
    fun bindsPresentAccountUpdateRequiredSheet(
        impl: RealPresentAccountUpdateRequiredSheet,
    ): PresentAccountUpdateRequiredSheet

    @Singleton
    @Binds
    fun bindsNavigationManager(
        impl: NavigationManagerImpl
    ): NavigationManager



    @Binds
    fun bindsHandleError(
        impl: RealHandleError
    ): HandleError

    companion object {
        @Provides
        @Singleton
        fun provideConsumersApiService(
            apiVersion: ApiVersion,
            stripeNetworkClient: StripeNetworkClient,
        ): ConsumersApiService = ConsumersApiServiceImpl(
            appInfo = null,
            sdkVersion = StripeSdkVersion.VERSION,
            apiVersion = apiVersion.code,
            stripeNetworkClient = stripeNetworkClient
        )

        @Singleton
        @Provides
        fun providesImageLoader(
            context: Application
        ) = StripeImageLoader(
            context = context,
            diskCache = null,
        )

        @Singleton
        @Provides
        fun providesFinancialConnectionsManifestRepository(
            requestExecutor: FinancialConnectionsRequestExecutor,
            apiRequestFactory: ApiRequest.Factory,
            apiOptions: ApiRequest.Options,
            locale: Locale?,
            logger: Logger,
            @Named(INITIAL_SYNC_RESPONSE) initialSynchronizeSessionResponse: SynchronizeSessionResponse?
        ) = FinancialConnectionsManifestRepository(
            requestExecutor = requestExecutor,
            apiRequestFactory = apiRequestFactory,
            apiOptions = apiOptions,
            locale = locale ?: Locale.getDefault(),
            logger = logger,
            initialSync = initialSynchronizeSessionResponse
        )

        @Singleton
        @Provides
        fun providesFinancialConnectionsConsumerSessionRepository(
            consumersApiService: ConsumersApiService,
            apiOptions: ApiRequest.Options,
            financialConnectionsConsumersApiService: FinancialConnectionsConsumersApiService,
            locale: Locale?,
            logger: Logger,
        ) = FinancialConnectionsConsumerSessionRepository(
            financialConnectionsConsumersApiService = financialConnectionsConsumersApiService,
            consumersApiService = consumersApiService,
            apiOptions = apiOptions,
            locale = locale ?: Locale.getDefault(),
            logger = logger,
        )

        @Singleton
        @Provides
        fun provideErrorReporter(
            context: Application,
            logger: Logger
        ): ErrorReporter {
            return SentryErrorReporter(
                context,
                logger = logger,
                sentryConfig = FinancialConnectionsSentryConfig
            )
        }

        @Singleton
        @Provides
        fun providesFinancialConnectionsAccountsRepository(
            requestExecutor: FinancialConnectionsRequestExecutor,
            apiOptions: ApiRequest.Options,
            apiRequestFactory: ApiRequest.Factory,
            logger: Logger,
            savedStateHandle: SavedStateHandle,
        ) = FinancialConnectionsAccountsRepository(
            requestExecutor = requestExecutor,
            apiRequestFactory = apiRequestFactory,
            apiOptions = apiOptions,
            logger = logger,
            savedStateHandle = savedStateHandle,
        )

        @Singleton
        @Provides
        fun providesFinancialConnectionsInstitutionsRepository(
            requestExecutor: FinancialConnectionsRequestExecutor,
            apiRequestFactory: ApiRequest.Factory,
            apiOptions: ApiRequest.Options
        ) = FinancialConnectionsInstitutionsRepository(
            requestExecutor = requestExecutor,
            apiOptions = apiOptions,
            apiRequestFactory = apiRequestFactory
        )

        @Provides
        internal fun provideFinancialConnectionsConsumersApiService(
            requestExecutor: FinancialConnectionsRequestExecutor,
            apiOptions: ApiRequest.Options,
            apiRequestFactory: ApiRequest.Factory,
        ): FinancialConnectionsConsumersApiService = FinancialConnectionsConsumersApiService(
            apiOptions = apiOptions,
            apiRequestFactory = apiRequestFactory,
            requestExecutor = requestExecutor
        )
    }
}
