package com.stripe.android.financialconnections.di

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetState
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewModel
import dagger.BindsInstance
import dagger.Subcomponent
import javax.inject.Singleton

@ActivityScoped
@Subcomponent(
    modules = [
        FinancialConnectionsSheetSharedModule::class,
    ]
)
internal interface FinancialConnectionsSheetSubcomponent {
    val viewModel: FinancialConnectionsSheetViewModel

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun initialState(initialState: FinancialConnectionsSheetState): Builder

        @BindsInstance
        fun configuration(configuration: FinancialConnectionsSheet.Configuration): Builder

        fun build(): FinancialConnectionsSheetSubcomponent
    }
}
