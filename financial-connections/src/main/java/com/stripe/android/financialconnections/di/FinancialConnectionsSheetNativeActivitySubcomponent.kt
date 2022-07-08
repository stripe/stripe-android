package com.stripe.android.financialconnections.di

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeState
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel
import dagger.BindsInstance
import dagger.Subcomponent

@ActivityScoped
@Subcomponent(
    modules = [
        FinancialConnectionsSheetNativeModule::class,
        FinancialConnectionsSheetSharedActivityModule::class
    ]
)
internal interface FinancialConnectionsSheetNativeActivitySubcomponent {
    val viewModel: FinancialConnectionsSheetNativeViewModel

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        fun initialState(initialState: FinancialConnectionsSheetNativeState): Builder

        @BindsInstance
        fun configuration(configuration: FinancialConnectionsSheet.Configuration): Builder

        fun build(): FinancialConnectionsSheetNativeActivitySubcomponent
    }
}
