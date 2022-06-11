package com.stripe.android.financialconnections.presentation

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.di.financialConnectionsSubComponentBuilderProvider
import com.stripe.android.financialconnections.domain.FlowCoordinator
import com.stripe.android.financialconnections.domain.FlowCoordinatorMessage
import com.stripe.android.financialconnections.domain.PostAuthorizationSession
import com.stripe.android.financialconnections.domain.SearchInstitutions
import com.stripe.android.financialconnections.model.Institution
import com.stripe.android.financialconnections.model.InstitutionResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class InstitutionPickerViewModel @Inject constructor(
    val configuration: FinancialConnectionsSheet.Configuration,
    val searchInstitutions: SearchInstitutions,
    val postAuthorizationSession: PostAuthorizationSession,
    private val flowCoordinator: FlowCoordinator,
    initialState: InstitutionPickerState
) : MavericksViewModel<InstitutionPickerState>(initialState) {

    private var searchJob: Job? = null

    init {
        suspend {
            searchInstitutions(
                clientSecret = configuration.financialConnectionsSessionClientSecret
            )
        }.execute(retainValue = InstitutionPickerState::institutions) { copy(institutions = it) }
    }

    fun onQueryChanged(query: String) {
        setState { copy(query = query) }
        searchJob?.cancel()
        searchJob = suspend {
            delay(SEARCH_DEBOUNCE_MS)
            searchInstitutions(
                clientSecret = configuration.financialConnectionsSessionClientSecret,
                query = query
            )
        }.execute(retainValue = InstitutionPickerState::institutions) { copy(institutions = it) }
    }

    fun onInstitutionSelected(institution: Institution) {
        viewModelScope.launch {
            postAuthorizationSession(institution.id)
            flowCoordinator.flow.emit(FlowCoordinatorMessage.OpenWebAuthFlow)
            // TODO@carlosmuvi use this when next steps available in native.
//            updateAuthSession(session)
//            requestNextStep(currentStep = NavigationDirections.institutionPicker)
        }
    }

    companion object :
        MavericksViewModelFactory<InstitutionPickerViewModel, InstitutionPickerState> {

        private const val SEARCH_DEBOUNCE_MS = 300L
        override fun create(
            viewModelContext: ViewModelContext,
            state: InstitutionPickerState
        ): InstitutionPickerViewModel {
            return viewModelContext.financialConnectionsSubComponentBuilderProvider
                .institutionPickerSubcomponentBuilder.get()
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

internal data class InstitutionPickerState(
    val query: String = "",
    val institutions: Async<InstitutionResponse> = Uninitialized
) : MavericksState
