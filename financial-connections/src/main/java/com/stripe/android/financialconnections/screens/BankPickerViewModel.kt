package com.stripe.android.financialconnections.screens

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.di.subComponentBuilderProvider
import com.stripe.android.financialconnections.domain.SearchInstitutions
import com.stripe.android.financialconnections.model.InstitutionResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import javax.inject.Inject

internal class BankPickerViewModel @Inject constructor(
    val configuration: FinancialConnectionsSheet.Configuration,
    val searchInstitutions: SearchInstitutions,
    initialState: BankPickerState
) : MavericksViewModel<BankPickerState>(initialState) {

    private var searchJob: Job? = null

    init {
        suspend { searchInstitutions() }
            .execute(retainValue = BankPickerState::institutions) { copy(institutions = it) }
    }

    fun onQueryChanged(query: String) {
        setState { copy(query = query) }
        searchJob?.cancel()
        searchJob = suspend {
            delay(300)
            searchInstitutions(query)
        }.execute(retainValue = BankPickerState::institutions) { copy(institutions = it) }
    }

    companion object : MavericksViewModelFactory<BankPickerViewModel, BankPickerState> {

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
    val query: String = "",
    val institutions: Async<InstitutionResponse> = Uninitialized
) : MavericksState