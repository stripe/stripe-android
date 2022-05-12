package com.stripe.android.financialconnections.screens

import android.util.Log
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.financialconnections.di.subComponentBuilderProvider
import com.stripe.android.financialconnections.domain.FetchFinancialConnectionsSession
import javax.inject.Inject

internal class BankPickerViewModel @Inject constructor(
    internal val testClass: TestClass,
    internal val fetchFinancialConnectionsSession: FetchFinancialConnectionsSession,
    initialState: BankPickerState
) : MavericksViewModel<BankPickerState>(initialState) {

    override fun onCleared() {
        super.onCleared()
        Log.e("CARLOS", "CLEARED!")
    }

    companion object :
        MavericksViewModelFactory<BankPickerViewModel, BankPickerState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: BankPickerState
        ): BankPickerViewModel {
            return viewModelContext.subComponentBuilderProvider
                .bankPickerSubComponentBuilder.get()
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

data class BankPickerState(
    val test: String = "test"
) : MavericksState