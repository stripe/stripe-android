package com.stripe.android.financialconnections.di

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetState
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewModel
import dagger.BindsInstance
import dagger.Component

@ActivityRetainedScope
@Component(
    dependencies = [FinancialConnectionsSingletonSharedComponent::class],
    modules = [
        FinancialConnectionsSheetModule::class,
        FinancialConnectionsSheetSharedModule::class,
    ]
)
internal interface FinancialConnectionsSheetComponent {
    val viewModel: FinancialConnectionsSheetViewModel

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun savedStateHandle(savedStateHandle: SavedStateHandle): Builder

        @BindsInstance
        fun initialState(initialState: FinancialConnectionsSheetState): Builder

        @BindsInstance
        fun configuration(configuration: FinancialConnectionsSheet.Configuration): Builder

        fun sharedComponent(component: FinancialConnectionsSingletonSharedComponent): Builder

        fun build(): FinancialConnectionsSheetComponent
    }
}
