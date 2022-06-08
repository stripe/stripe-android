package com.stripe.android.financialconnections.di

import com.stripe.android.financialconnections.navigation.NavigationManager
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
internal class NavigationModule {

    @Singleton
    @Provides
    fun providesNavigationManager() = NavigationManager(
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    )
}
