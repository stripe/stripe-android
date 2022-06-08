package com.stripe.android.financialconnections.di

import android.app.Application
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.LoggingModule
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeState
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        FinancialConnectionsSheetNativeModule::class,
        FinancialConnectionsSheetModule::class,
        LoggingModule::class,
        CoroutineContextModule::class,
        NavigationModule::class
    ]
)
internal interface FinancialConnectionsSheetNativeComponent {
    val viewModel: FinancialConnectionsSheetNativeViewModel

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun initialState(initialState: FinancialConnectionsSheetNativeState): Builder

        @BindsInstance
        fun internalArgs(financialConnectionsSheetActivityArgs: FinancialConnectionsSheetActivityArgs): Builder

        fun build(): FinancialConnectionsSheetNativeComponent
    }
}
