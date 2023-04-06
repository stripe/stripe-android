package com.stripe.android.financialconnections.features.institutionpicker

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.InstitutionSelected
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.SearchSucceeded
import com.stripe.android.financialconnections.domain.FeaturedInstitutions
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.SearchInstitutions
import com.stripe.android.financialconnections.domain.UpdateLocalManifest
import com.stripe.android.financialconnections.features.institutionpicker.InstitutionPickerState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.utils.ConflatedJob
import com.stripe.android.financialconnections.utils.isCancellationError
import com.stripe.android.financialconnections.utils.measureTimeMillis
import kotlinx.coroutines.delay
import javax.inject.Inject

@Suppress("LongParameterList")
internal class InstitutionPickerViewModel @Inject constructor(
    private val configuration: FinancialConnectionsSheet.Configuration,
    private val searchInstitutions: SearchInstitutions,
    private val featuredInstitutions: FeaturedInstitutions,
    private val getManifest: GetManifest,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
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
            val institutions = kotlin.runCatching {
                featuredInstitutions(
                    clientSecret = configuration.financialConnectionsSessionClientSecret
                )
            }.onFailure {
                logger.error("Error fetching featured institutions", it)
                eventTracker.track(FinancialConnectionsEvent.Error(Pane.INSTITUTION_PICKER, it))
            }
            val institutionsResult = institutions.getOrNull()?.data ?: emptyList()
            Payload(
                featuredInstitutions = institutionsResult,
                searchDisabled = manifest.institutionSearchDisabled,
                allowManualEntry = manifest.allowManualEntry
            )
        }.execute { copy(payload = it) }
    }

    private fun logErrors() {
        onAsync(
            InstitutionPickerState::payload,
            onSuccess = { eventTracker.track(PaneLoaded(Pane.INSTITUTION_PICKER)) },
            onFail = {
                logger.error("Error fetching initial payload", it)
                eventTracker.track(FinancialConnectionsEvent.Error(Pane.INSTITUTION_PICKER, it))
            }
        )
        onAsync(
            InstitutionPickerState::searchInstitutions,
            onFail = {
                logger.error("Error searching institutions", it)
                eventTracker.track(FinancialConnectionsEvent.Error(Pane.INSTITUTION_PICKER, it))
            }
        )
    }

    fun onQueryChanged(query: String) {
        searchJob += suspend {
            if (query.isNotBlank()) {
                delay(SEARCH_DEBOUNCE_MS)
                val (result, millis) = measureTimeMillis {
                    searchInstitutions(
                        clientSecret = configuration.financialConnectionsSessionClientSecret,
                        query = query
                    )
                }
                eventTracker.track(
                    SearchSucceeded(
                        pane = Pane.INSTITUTION_PICKER,
                        query = query,
                        duration = millis,
                        resultCount = result.data.count()
                    )
                )
                result
            } else {
                InstitutionResponse(
                    data = emptyList(),
                    showManualEntry = false
                )
            }
        }.execute {
            copy(searchInstitutions = if (it.isCancellationError()) Loading() else it)
        }
    }

    fun onInstitutionSelected(institution: FinancialConnectionsInstitution, fromFeatured: Boolean) {
        clearSearch()
        suspend {
            eventTracker.track(
                InstitutionSelected(
                    pane = Pane.INSTITUTION_PICKER,
                    fromFeatured = fromFeatured,
                    institutionId = institution.id
                )
            )
            // updates local manifest with active institution and cleans auth session if present.
            updateLocalManifest {
                it.copy(
                    activeInstitution = institution,
                    activeAuthSession = null
                )
            }
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
                searchInstitutions = Success(
                    InstitutionResponse(
                        data = emptyList(),
                        showManualEntry = false
                    )
                ),
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
    // This is just used to provide a text in Compose previews
    val previewText: String? = null,
    val searchMode: Boolean = false,
    val payload: Async<Payload> = Uninitialized,
    val searchInstitutions: Async<InstitutionResponse> = Uninitialized
) : MavericksState {

    data class Payload(
        val featuredInstitutions: List<FinancialConnectionsInstitution>,
        val allowManualEntry: Boolean,
        val searchDisabled: Boolean
    )
}
