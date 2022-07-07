package com.stripe.android.financialconnections.di

import com.stripe.android.financialconnections.features.consent.ConsentSubcomponent
import com.stripe.android.financialconnections.features.institutionpicker.InstitutionPickerSubcomponent
import com.stripe.android.financialconnections.navigation.NavigationManager
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module(
    subcomponents = [
        ConsentSubcomponent::class,
        InstitutionPickerSubcomponent::class
    ]
)
internal class FinancialConnectionsSheetNativeModule {

    @ActivityScoped
    @Provides
    fun providesNavigationManager() = NavigationManager(
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    )
}
