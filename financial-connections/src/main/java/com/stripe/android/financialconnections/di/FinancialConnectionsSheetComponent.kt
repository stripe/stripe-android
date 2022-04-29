package com.stripe.android.financialconnections.di

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.LoggingModule
import com.stripe.android.financialconnections.FinancialConnectionsSheetContract
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewModel
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        FinancialConnectionsSheetModule::class,
        CoroutineContextModule::class,
        LoggingModule::class
    ]
)
internal interface FinancialConnectionsSheetComponent {
    val viewModel: FinancialConnectionsSheetViewModel

    fun inject(factory: FinancialConnectionsSheetViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun savedStateHandle(savedStateHandle: SavedStateHandle): Builder

        @BindsInstance
        fun configuration(configuration: FinancialConnectionsSheetContract.Args): Builder

        fun build(): FinancialConnectionsSheetComponent
    }
}
