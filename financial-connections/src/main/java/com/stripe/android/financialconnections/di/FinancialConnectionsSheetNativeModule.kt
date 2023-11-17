package com.stripe.android.financialconnections.di

import android.app.Application
import com.stripe.android.core.ApiVersion
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerSubcomponent
import com.stripe.android.financialconnections.features.attachpayment.AttachPaymentSubcomponent
import com.stripe.android.financialconnections.features.consent.ConsentSubcomponent
import com.stripe.android.financialconnections.features.institutionpicker.InstitutionPickerSubcomponent
import com.stripe.android.financialconnections.features.manualentry.ManualEntrySubcomponent
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthSubcomponent
import com.stripe.android.financialconnections.features.reset.ResetSubcomponent
import com.stripe.android.financialconnections.features.success.SuccessSubcomponent
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.NavigationManagerImpl
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.financialconnections.repository.CoreAuthorizationPendingNetworkingRepairRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsConsumerSessionRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsInstitutionsRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import com.stripe.android.financialconnections.repository.SaveToLinkWithStripeSucceededRepository
import com.stripe.android.financialconnections.repository.api.FinancialConnectionsConsumersApiService
import com.stripe.android.repository.ConsumersApiService
import com.stripe.android.repository.ConsumersApiServiceImpl
import com.stripe.android.uicore.image.StripeImageLoader
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.util.Locale
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module(
    subcomponents = [
        ConsentSubcomponent::class,
        ManualEntrySubcomponent::class,
        InstitutionPickerSubcomponent::class,
        PartnerAuthSubcomponent::class,
        SuccessSubcomponent::class,
        AccountPickerSubcomponent::class,
        AttachPaymentSubcomponent::class,
        ResetSubcomponent::class
    ]
)
internal interface FinancialConnectionsSheetNativeModule {

    @Singleton
    @Binds
    fun providesNavigationManager(
        impl: NavigationManagerImpl
    ): NavigationManager

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
        fun providesFinancialConnectionsAccountsRepository(
            requestExecutor: FinancialConnectionsRequestExecutor,
            apiOptions: ApiRequest.Options,
            apiRequestFactory: ApiRequest.Factory,
            logger: Logger
        ) = FinancialConnectionsAccountsRepository(
            requestExecutor = requestExecutor,
            apiRequestFactory = apiRequestFactory,
            apiOptions = apiOptions,
            logger = logger
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

        @Singleton
        @Provides
        fun providesSaveToLinkWithStripeSucceededRepository(
            @IOContext workContext: CoroutineContext
        ) = SaveToLinkWithStripeSucceededRepository(
            CoroutineScope(SupervisorJob() + workContext)
        )

        @Singleton
        @Provides
        fun providesPartnerToCoreAuthsRepository(
            logger: Logger,
            @IOContext workContext: CoroutineContext,
            analyticsTracker: FinancialConnectionsAnalyticsTracker
        ) = CoreAuthorizationPendingNetworkingRepairRepository(
            coroutineScope = CoroutineScope(SupervisorJob() + workContext),
            logger = logger,
            analyticsTracker = analyticsTracker
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
