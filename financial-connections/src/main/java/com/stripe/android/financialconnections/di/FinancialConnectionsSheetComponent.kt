package com.stripe.android.financialconnections.di

import android.app.Application
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetState
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewModel
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        FinancialConnectionsSheetModule::class,
        FinancialConnectionsSheetSharedModule::class,
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
        fun configuration(configuration: FinancialConnectionsSheet.Configuration): Builder

        fun build(): FinancialConnectionsSheetComponent
    }
}
