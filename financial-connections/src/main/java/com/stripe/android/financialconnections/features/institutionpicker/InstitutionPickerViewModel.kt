package com.stripe.android.financialconnections.features.institutionpicker

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.di.financialConnectionsSubComponentBuilderProvider
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message
import com.stripe.android.financialconnections.domain.PostAuthorizationSession
import com.stripe.android.financialconnections.domain.SearchInstitutions
import com.stripe.android.financialconnections.model.Institution
import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.navigation.NavigationDirections
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import javax.inject.Inject

internal class InstitutionPickerViewModel @Inject constructor(
    private val configuration: FinancialConnectionsSheet.Configuration,
    private val searchInstitutions: SearchInstitutions,
    private val postAuthorizationSession: PostAuthorizationSession,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val logger: Logger,
    initialState: InstitutionPickerState
) : MavericksViewModel<InstitutionPickerState>(initialState) {

    private var searchJob: Job? = null

    init {
        logErrors()
        suspend {
            searchInstitutions(
                clientSecret = configuration.financialConnectionsSessionClientSecret
            )
        }.execute { copy(featuredInstitutions = it) }
    }

    private fun logErrors() {
        onAsync(InstitutionPickerState::selectInstitution, onFail = {
            logger.error("Error selecting institution", it)
        })
        onAsync(InstitutionPickerState::featuredInstitutions, onFail = {
            logger.error("Error fetching featured institutions", it)
        })
        onAsync(InstitutionPickerState::searchInstitutions, onFail = {
            logger.error("Error searching institutions", it)
        })
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
        }.execute { copy(searchInstitutions = it) }
    }

    fun onInstitutionSelected(institution: Institution) {
        suspend {
            // api call
            val session = postAuthorizationSession(institution.id)
            // navigate to next step
            nativeAuthFlowCoordinator().emit(Message.UpdateAuthorizationSession(session))
            nativeAuthFlowCoordinator().emit(Message.RequestNextStep(NavigationDirections.institutionPicker))
        }.execute {
            copy(selectInstitution = it)
        }
    }

    fun onCancelSearchClick() {
        setState {
            copy(
                query = "",
                searchInstitutions = Success(InstitutionResponse(emptyList())),
                searchMode = false
            )
        }
    }

    fun onSearchFocused() {
        setState {
            copy(searchMode = true)
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
    val searchMode: Boolean = false,
    val featuredInstitutions: Async<InstitutionResponse> = Uninitialized,
    val searchInstitutions: Async<InstitutionResponse> = Uninitialized,
    val selectInstitution: Async<Unit> = Uninitialized
) : MavericksState
