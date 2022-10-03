package com.stripe.android.financialconnections.features.institutionpicker

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.domain.FeaturedInstitutions
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.SearchInstitutions
import com.stripe.android.financialconnections.domain.UpdateLocalManifest
import com.stripe.android.financialconnections.features.institutionpicker.InstitutionPickerState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.utils.ConflatedJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import javax.inject.Inject

@Suppress("LongParameterList")
internal class InstitutionPickerViewModel @Inject constructor(
    private val configuration: FinancialConnectionsSheet.Configuration,
    private val searchInstitutions: SearchInstitutions,
    private val featuredInstitutions: FeaturedInstitutions,
    private val getManifest: GetManifest,
    private val navigationManager: NavigationManager,
    private val updateLocalManifest: UpdateLocalManifest,
    private val logger: Logger,
    initialState: InstitutionPickerState
) : MavericksViewModel<InstitutionPickerState>(initialState) {

    private var searchJob = ConflatedJob()

    init {
        logErrors()
        suspend {
            val manifest = getManifest()
            Payload(
                featuredInstitutions = featuredInstitutions(
                    clientSecret = configuration.financialConnectionsSessionClientSecret
                ),
                searchDisabled = manifest.institutionSearchDisabled,
                allowManualEntry = kotlin
                    .runCatching { manifest.allowManualEntry }
                    .getOrElse { false }
            )
        }.execute { copy(payload = it) }
    }

    private fun logErrors() {
        onAsync(InstitutionPickerState::payload, onFail = {
            logger.error("Error fetching initial payload", it)
        })
        onAsync(InstitutionPickerState::searchInstitutions, onFail = {
            logger.error("Error searching institutions", it)
        })
    }

    fun onQueryChanged(query: String) {
        setState { copy(query = query) }
        searchJob += suspend {
            delay(SEARCH_DEBOUNCE_MS)
            searchInstitutions(
                clientSecret = configuration.financialConnectionsSessionClientSecret,
                query = query
            )
        }.execute {
            copy(searchInstitutions = if (it.isCancellationError()) Loading() else it)
        }
    }

    /**
     * Prevents [CancellationException] to map to [Fail] when coroutine being cancelled
     * due to search query changes. In these cases, re-map the [Async] instance to [Loading]
     */
    private fun Async<InstitutionResponse>.isCancellationError(): Boolean = when {
        this !is Fail -> false
        error is CancellationException -> true
        error is StripeException && error.cause is CancellationException -> true
        else -> false
    }

    fun onInstitutionSelected(institution: FinancialConnectionsInstitution) {
        clearSearch()
        suspend {
            // updates local manifest with active institution
            updateLocalManifest { it.copy(activeInstitution = institution) }
            // navigate to next step
            navigationManager.navigate(NavigationDirections.partnerAuth)
        }.execute { this }
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
    val searchInstitutions: Async<InstitutionResponse> = Uninitialized
) : MavericksState {
    data class Payload(
        val featuredInstitutions: InstitutionResponse,
        val allowManualEntry: Boolean,
        val searchDisabled: Boolean
    )
}
