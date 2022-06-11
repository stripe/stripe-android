package com.stripe.android.financialconnections.di

import com.stripe.android.financialconnections.domain.ManifestFlow
import com.stripe.android.financialconnections.domain.ObserveManifestUpdates
import com.stripe.android.financialconnections.domain.UpdateManifest
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.screens.ConsentSubcomponent
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module(
    subcomponents = [
        ConsentSubcomponent::class
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
    fun providesUpdateManifest(manifestFlow: ManifestFlow) : UpdateManifest {
        return manifestFlow
    }

    @Singleton
    @Provides
    fun providesObserveManifest(manifestFlow: ManifestFlow) : ObserveManifestUpdates {
        return manifestFlow
    }
}
