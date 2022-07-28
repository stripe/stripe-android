package com.stripe.android.financialconnections.di

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerSubcomponent
import com.stripe.android.financialconnections.features.consent.ConsentSubcomponent
import com.stripe.android.financialconnections.features.institutionpicker.InstitutionPickerSubcomponent
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthSubcomponent
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.financialconnections.repository.FinancialConnectionsInstitutionsRepository
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
        InstitutionPickerSubcomponent::class,
        PartnerAuthSubcomponent::class,
        AccountPickerSubcomponent::class
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
    fun providesFinancialConnectionsInstitutionsRepository(
        @Named(PUBLISHABLE_KEY) publishableKey: String,
        requestExecutor: FinancialConnectionsRequestExecutor,
        apiRequestFactory: ApiRequest.Factory
    ) = FinancialConnectionsInstitutionsRepository(
        publishableKey,
        requestExecutor,
        apiRequestFactory
    )
}
