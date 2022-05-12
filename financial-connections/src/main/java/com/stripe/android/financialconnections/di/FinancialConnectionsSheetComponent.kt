package com.stripe.android.financialconnections.di

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.LoggingModule
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.FinancialConnectionsSheetState
import com.stripe.android.financialconnections.FinancialConnectionsSheetViewModel
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityArgs
import com.stripe.android.financialconnections.screens.BankPickerViewModel
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

    fun inject(factory: FinancialConnectionsSheetViewModel.Companion)
    fun inject(factory: BankPickerViewModel.Companion)

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
