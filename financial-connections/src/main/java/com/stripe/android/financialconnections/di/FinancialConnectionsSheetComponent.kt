package com.stripe.android.financialconnections.di

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
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

    @Component.Factory
    interface Factory {
        fun build(
            @BindsInstance application: Application,
            @BindsInstance savedStateHandle: SavedStateHandle,
            @BindsInstance initialState: FinancialConnectionsSheetState,
            @BindsInstance configuration: FinancialConnectionsSheetConfiguration,
            sharedComponent: FinancialConnectionsSingletonSharedComponent,
        ): FinancialConnectionsSheetComponent
    }
}
