package com.stripe.android.financialconnections.di

import android.app.Application
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerSubcomponent
import com.stripe.android.financialconnections.features.attachpayment.AttachPaymentSubcomponent
import com.stripe.android.financialconnections.features.consent.ConsentSubcomponent
import com.stripe.android.financialconnections.features.institutionpicker.InstitutionPickerSubcomponent
import com.stripe.android.financialconnections.features.manualentry.ManualEntrySubcomponent
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthSubcomponent
import com.stripe.android.financialconnections.features.reset.ResetSubcomponent
import com.stripe.android.financialconnections.features.success.SuccessSubcomponent
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.financialconnections.repository.FinancialConnectionsAccountsRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsInstitutionsRepository
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import com.stripe.android.uicore.image.StripeImageLoader
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Named
import javax.inject.Singleton

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
internal class FinancialConnectionsSheetNativeModule {

    @Singleton
    @Provides
    fun providesNavigationManager() = NavigationManager(
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
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
        configuration: FinancialConnectionsSheet.Configuration,
        apiRequestFactory: ApiRequest.Factory,
        apiOptions: ApiRequest.Options,
        logger: Logger,
        @Named(INITIAL_MANIFEST) initialManifest: FinancialConnectionsSessionManifest
    ) = FinancialConnectionsManifestRepository(
        requestExecutor = requestExecutor,
        configuration = configuration,
        apiRequestFactory = apiRequestFactory,
        apiOptions = apiOptions,
        logger = logger,
        initialManifest = initialManifest
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
}
