package com.stripe.android.financialconnections.di

import com.stripe.android.financialconnections.domain.FlowCoordinator
import com.stripe.android.financialconnections.domain.ObserveFlowUpdates
import com.stripe.android.financialconnections.navigation.NavigationManager
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module(
    subcomponents = [
        ConsentSubcomponent::class,
        InstitutionPickerSubcomponent::class
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
    fun providesUpdateManifest(flowCoordinator: FlowCoordinator): ObserveFlowUpdates {
        return flowCoordinator
    }
}
