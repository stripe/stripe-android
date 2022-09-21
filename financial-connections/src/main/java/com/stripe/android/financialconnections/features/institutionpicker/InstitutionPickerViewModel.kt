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
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.domain.PostAuthorizationSession
import com.stripe.android.financialconnections.domain.SearchInstitutions
import com.stripe.android.financialconnections.features.institutionpicker.InstitutionPickerState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class InstitutionPickerViewModel @Inject constructor(
    private val configuration: FinancialConnectionsSheet.Configuration,
    private val searchInstitutions: SearchInstitutions,
    private val getManifest: GetManifest,
    private val postAuthorizationSession: PostAuthorizationSession,
    private val navigationManager: NavigationManager,
    private val goNext: GoNext,
    private val logger: Logger,
    initialState: InstitutionPickerState
) : MavericksViewModel<InstitutionPickerState>(initialState) {

    private var searchJob: Job? = null

    init {
        logErrors()
        suspend {
            val manifest = getManifest()
            Payload(
                featuredInstitutions = searchInstitutions(
                    clientSecret = configuration.financialConnectionsSessionClientSecret
                ),
                allowManualEntry = kotlin
                    .runCatching { manifest.allowManualEntry }
                    .getOrElse { false }
            )
        }.execute { copy(payload = it) }
    }

    private fun logErrors() {
        onAsync(InstitutionPickerState::selectInstitution, onFail = {
            logger.error("Error selecting institution", it)
        })
        onAsync(InstitutionPickerState::payload, onFail = {
            logger.error("Error fetching initial payload", it)
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

    fun onInstitutionSelected(institution: FinancialConnectionsInstitution) {
        clearSearch()
        suspend {
            // api call
            val authSession = postAuthorizationSession(institution)
            // navigate to next step
            goNext(authSession.nextPane)
            Unit
        }.execute {
            copy(
                selectInstitution = it,
            )
        }
    }

    fun onCancelSearchClick() {
        clearSearch()
    }

    private fun clearSearch() {
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

    fun onSelectAnotherBank() {
        setState {
            copy(
                selectInstitution = Uninitialized
            )
        }
    }

    fun onManualEntryClick() {
        navigationManager.navigate(NavigationDirections.manualEntry)
    }

    companion object :
        MavericksViewModelFactory<InstitutionPickerViewModel, InstitutionPickerState> {

        private const val SEARCH_DEBOUNCE_MS = 300L
        override fun create(
            viewModelContext: ViewModelContext,
            state: InstitutionPickerState
        ): InstitutionPickerViewModel {
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .institutionPickerBuilder
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

internal data class InstitutionPickerState(
    val query: String = "",
    val searchMode: Boolean = false,
    val allowManualEntry: Boolean = false,
    val payload: Async<Payload> = Uninitialized,
    val searchInstitutions: Async<InstitutionResponse> = Uninitialized,
    val selectInstitution: Async<Unit> = Uninitialized
) : MavericksState {
    data class Payload(
        val featuredInstitutions: InstitutionResponse,
        val allowManualEntry: Boolean
    )
}
