package com.stripe.android.financialconnections.di

import android.app.Application
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.financialconnections.FinancialConnectionsSheetState
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewModel
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        FinancialConnectionsSheetModule::class,
        CoroutineContextModule::class,
        CoreCommonModule::class
    ]
)
internal interface FinancialConnectionsSheetComponent {
    val viewModel: FinancialConnectionsSheetViewModel

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun initialState(initialState: FinancialConnectionsSheetState): Builder

        @BindsInstance
        fun internalArgs(financialConnectionsSheetActivityArgs: FinancialConnectionsSheetActivityArgs): Builder

        fun build(): FinancialConnectionsSheetComponent
    }
}
