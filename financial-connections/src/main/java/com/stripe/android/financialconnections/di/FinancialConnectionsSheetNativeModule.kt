package com.stripe.android.financialconnections.di

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.ApiVersion
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.financialconnections.domain.AttachConsumerToLinkAccountSession
import com.stripe.android.financialconnections.domain.HandleError
import com.stripe.android.financialconnections.domain.RealAttachConsumerToLinkAccountSession
import com.stripe.android.financialconnections.domain.RealHandleError
import com.stripe.android.financialconnections.domain.RealSignUpToLink
import com.stripe.android.financialconnections.domain.SignUpToLink
import com.stripe.android.financialconnections.features.accountupdate.PresentAccountUpdateRequiredSheet
import com.stripe.android.financialconnections.features.accountupdate.RealPresentAccountUpdateRequiredSheet
import com.stripe.android.financialconnections.features.notice.PresentSheet
import com.stripe.android.financialconnections.features.notice.RealPresentSheet
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.NavigationManagerImpl
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.financialconnections.repository.ConsumerSessionRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsInstitutionsRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import com.stripe.android.financialconnections.repository.api.FinancialConnectionsConsumersApiService
import com.stripe.android.financialconnections.repository.api.ProvideApiRequestOptions
import com.stripe.android.financialconnections.repository.api.RealProvideApiRequestOptions
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
    fun bindsPresentNoticeSheet(impl: RealPresentSheet): PresentSheet

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

    @Binds
    @Singleton
    fun bindsProvideApiRequestOptions(impl: RealProvideApiRequestOptions): ProvideApiRequestOptions

    @Binds
    fun bindsAttachConsumerToLinkAccountSession(
        impl: RealAttachConsumerToLinkAccountSession,
    ): AttachConsumerToLinkAccountSession

    @Binds
    fun bindsSignUpToLink(
        impl: RealSignUpToLink,
    ): SignUpToLink

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
            provideApiRequestOptions: ProvideApiRequestOptions,
            locale: Locale?,
            logger: Logger,
            @Named(INITIAL_SYNC_RESPONSE) initialSynchronizeSessionResponse: SynchronizeSessionResponse?
        ) = FinancialConnectionsManifestRepository(
            requestExecutor = requestExecutor,
            apiRequestFactory = apiRequestFactory,
            provideApiRequestOptions = provideApiRequestOptions,
            locale = locale ?: Locale.getDefault(),
            logger = logger,
            initialSync = initialSynchronizeSessionResponse
        )

        @Singleton
        @Provides
        fun providesFinancialConnectionsConsumerSessionRepository(
            consumersApiService: ConsumersApiService,
            provideApiRequestOptions: ProvideApiRequestOptions,
            financialConnectionsConsumersApiService: FinancialConnectionsConsumersApiService,
            consumerSessionRepository: ConsumerSessionRepository,
            locale: Locale?,
            logger: Logger,
        ) = FinancialConnectionsConsumerSessionRepository(
            financialConnectionsConsumersApiService = financialConnectionsConsumersApiService,
            provideApiRequestOptions = provideApiRequestOptions,
            consumersApiService = consumersApiService,
            consumerSessionRepository = consumerSessionRepository,
            locale = locale ?: Locale.getDefault(),
            logger = logger,
        )

        @Singleton
        @Provides
        fun providesFinancialConnectionsAccountsRepository(
            requestExecutor: FinancialConnectionsRequestExecutor,
            provideApiRequestOptions: ProvideApiRequestOptions,
            apiRequestFactory: ApiRequest.Factory,
            logger: Logger,
            savedStateHandle: SavedStateHandle,
        ) = FinancialConnectionsAccountsRepository(
            requestExecutor = requestExecutor,
            provideApiRequestOptions = provideApiRequestOptions,
            apiRequestFactory = apiRequestFactory,
            logger = logger,
            savedStateHandle = savedStateHandle,
        )

        @Singleton
        @Provides
        fun providesFinancialConnectionsInstitutionsRepository(
            requestExecutor: FinancialConnectionsRequestExecutor,
            provideApiRequestOptions: ProvideApiRequestOptions,
            apiRequestFactory: ApiRequest.Factory,
        ) = FinancialConnectionsInstitutionsRepository(
            requestExecutor = requestExecutor,
            provideApiRequestOptions = provideApiRequestOptions,
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
