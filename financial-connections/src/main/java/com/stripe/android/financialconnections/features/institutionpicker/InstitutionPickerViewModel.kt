package com.stripe.android.financialconnections.features.institutionpicker

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.FeaturedInstitutionsLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.InstitutionSelected
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.SearchScroll
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.SearchSucceeded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Metadata
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.core.Async
import com.stripe.android.financialconnections.core.Async.Loading
import com.stripe.android.financialconnections.core.Async.Uninitialized
import com.stripe.android.financialconnections.core.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.FeaturedInstitutions
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.HandleError
import com.stripe.android.financialconnections.domain.PostAuthorizationSession
import com.stripe.android.financialconnections.domain.SearchInstitutions
import com.stripe.android.financialconnections.domain.UpdateLocalManifest
import com.stripe.android.financialconnections.features.institutionpicker.InstitutionPickerState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.navigation.Destination.ManualEntry
import com.stripe.android.financialconnections.navigation.Destination.PartnerAuth
import com.stripe.android.financialconnections.navigation.Destination.PartnerAuthDrawer
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.utils.ConflatedJob
import com.stripe.android.financialconnections.utils.isCancellationError
import com.stripe.android.financialconnections.utils.measureTimeMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class InstitutionPickerViewModel @Inject constructor(
    private val configuration: FinancialConnectionsSheet.Configuration,
    private val postAuthorizationSession: PostAuthorizationSession,
    private val getOrFetchSync: GetOrFetchSync,
    private val searchInstitutions: SearchInstitutions,
    private val featuredInstitutions: FeaturedInstitutions,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val handleError: HandleError,
    private val navigationManager: NavigationManager,
    private val updateLocalManifest: UpdateLocalManifest,
    private val logger: Logger,
    initialState: InstitutionPickerState
) : FinancialConnectionsViewModel<InstitutionPickerState>(initialState) {

    private var searchJob = ConflatedJob()

    init {
        suspend {
            val manifest = getOrFetchSync().manifest
            val (featuredInstitutions: InstitutionResponse, duration: Long) = runCatching {
                measureTimeMillis {
                    featuredInstitutions(
                        clientSecret = configuration.financialConnectionsSessionClientSecret
                    )
                }
            }.onFailure {
                eventTracker.logError(
                    extraMessage = "Error fetching featured institutions",
                    error = it,
                    pane = PANE,
                    logger = logger
                )
            }.getOrElse {
                // Allow users to search for institutions even if featured institutions fails.
                InstitutionResponse(
                    data = emptyList(),
                    showManualEntry = manifest.allowManualEntry
                ) to 0L
            }
            Payload(
                featuredInstitutionsDuration = duration,
                featuredInstitutions = featuredInstitutions,
                searchDisabled = manifest.institutionSearchDisabled,
            )
        }.execute(
            reducer = { result -> copy(payload = result) },
            onSuccess = { payload ->
                eventTracker.track(PaneLoaded(PANE))
                eventTracker.track(
                    FeaturedInstitutionsLoaded(
                        pane = PANE,
                        duration = payload.featuredInstitutionsDuration,
                        institutionIds = payload.featuredInstitutions.data.map { it.id }.toSet()
                    )
                )
            },
            onFail = {
                handleError(
                    extraMessage = "Error fetching initial payload",
                    error = it,
                    pane = PANE,
                    displayErrorScreen = true
                )
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
                        pane = PANE,
                        query = query,
                        duration = millis,
                        resultCount = result.data.count()
                    )
                )
                FinancialConnections.emitEvent(Name.SEARCH_INITIATED)
                result
            } else {
                InstitutionResponse(
                    data = emptyList(),
                    showManualEntry = false
                )
            }
        }.execute(
            reducer = { copy(searchInstitutions = if (it.isCancellationError()) Loading else it) },
            onFail = {
                handleError(
                    extraMessage = "Error searching institutions",
                    error = it,
                    pane = PANE,
                    displayErrorScreen = false // don't show error screen for search errors.
                )
            }
        )
    }

    fun onInstitutionSelected(institution: FinancialConnectionsInstitution, fromFeatured: Boolean) {
        suspend {
            eventTracker.track(
                InstitutionSelected(
                    pane = PANE,
                    fromFeatured = fromFeatured,
                    institutionId = institution.id
                )
            )
            FinancialConnections.emitEvent(
                name = Name.INSTITUTION_SELECTED,
                metadata = Metadata(institutionName = institution.name)
            )
            // updates local manifest with active institution and cleans auth session if present.
            updateLocalManifest {
                it.copy(
                    activeInstitution = institution,
                    activeAuthSession = null
                )
            }
            // navigate to next step
            val authSession = postAuthorizationSession(institution, getOrFetchSync())
            navigateToPartnerAuth(authSession)
        }.execute(
            reducer = { result ->
                copy(
                    selectedInstitutionId = institution.id.takeIf { result is Loading },
                    createSessionForInstitution = result
                )
            },
            onFail = {
                handleError(
                    extraMessage = "Error selecting or creating session for institution",
                    error = it,
                    pane = PANE,
                    displayErrorScreen = true
                )
            }
        )
    }

    /**
     * Navigates to the partner auth screen:
     *
     * - If the [authSession] is OAuth, it will navigate to [PartnerAuthDrawer]. The pre-pane will show
     *   as a bottom sheet, where users can accept which will open the browser with the bank OAuth login.
     * - If the [authSession] is not-OAuth, it will navigate to [PartnerAuth] (full screen).
     *   non-OAuth sessions don't have a pre-pane, so partner auth will show a full-screen loading
     *   and open the browser right away.
     */
    private fun navigateToPartnerAuth(authSession: FinancialConnectionsAuthorizationSession) {
        navigationManager.tryNavigateTo(
            if (authSession.isOAuth) {
                PartnerAuthDrawer(referrer = PANE)
            } else {
                PartnerAuth(referrer = PANE)
            }
        )
    }

    fun onManualEntryClick() {
        navigationManager.tryNavigateTo(ManualEntry(referrer = PANE))
    }

    fun onScrollChanged() {
        viewModelScope.launch {
            eventTracker.track(
                SearchScroll(
                    pane = PANE,
                    institutionIds = stateFlow.value.searchInstitutions()
                        ?.data
                        ?.map { it.id }
                        ?.toSet() ?: emptySet(),
                )
            )
        }
    }

    companion object {
        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    parentComponent
                        .institutionPickerBuilder
                        .initialState(InstitutionPickerState())
                        .build()
                        .viewModel
                }
            }

        private const val SEARCH_DEBOUNCE_MS = 300L
        private val PANE = Pane.INSTITUTION_PICKER
    }
}

internal data class InstitutionPickerState(
    // This is just used to provide a text in Compose previews
    val previewText: String? = null,
    val selectedInstitutionId: String? = null,
    val payload: Async<Payload> = Uninitialized,
    val searchInstitutions: Async<InstitutionResponse> = Uninitialized,
    val createSessionForInstitution: Async<Unit> = Uninitialized
) {

    data class Payload(
        val featuredInstitutions: InstitutionResponse,
        val searchDisabled: Boolean,
        val featuredInstitutionsDuration: Long
    )
}
