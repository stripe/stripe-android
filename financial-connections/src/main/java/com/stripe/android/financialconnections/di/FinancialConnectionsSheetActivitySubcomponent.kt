package com.stripe.android.financialconnections.di

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetState
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewModel
import dagger.BindsInstance
import dagger.Subcomponent

@ActivityScoped
@Subcomponent(
    modules = [
        FinancialConnectionsSheetSharedActivityModule::class
    ]
)
internal interface FinancialConnectionsSheetActivitySubcomponent {
    val viewModel: FinancialConnectionsSheetViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: FinancialConnectionsSheetState): Builder

        @BindsInstance
        fun configuration(configuration: FinancialConnectionsSheet.Configuration): Builder

        fun build(): FinancialConnectionsSheetActivitySubcomponent
    }
}
