package com.stripe.android.financialconnections.features.institutionpicker

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
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
import com.stripe.android.financialconnections.domain.FeaturedInstitutions
import com.stripe.android.financialconnections.domain.GetOrFetchSync
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
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.utils.ConflatedJob
import com.stripe.android.financialconnections.utils.isCancellationError
import com.stripe.android.financialconnections.utils.measureTimeMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
internal class InstitutionPickerViewModel @Inject constructor(
    private val configuration: FinancialConnectionsSheet.Configuration,
    private val postAuthorizationSession: PostAuthorizationSession,
    private val getOrFetchSync: GetOrFetchSync,
    private val searchInstitutions: SearchInstitutions,
    private val featuredInstitutions: FeaturedInstitutions,
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
                    pane = Pane.INSTITUTION_PICKER,
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
        }.execute { copy(payload = it) }
    }

    private fun logErrors() {
        onAsync(
            InstitutionPickerState::payload,
            onSuccess = { payload ->
                eventTracker.track(PaneLoaded(Pane.INSTITUTION_PICKER))
                eventTracker.track(
                    FeaturedInstitutionsLoaded(
                        pane = Pane.INSTITUTION_PICKER,
                        duration = payload.featuredInstitutionsDuration,
                        institutionIds = payload.featuredInstitutions.data.map { it.id }.toSet()
                    )
                )
            },
            onFail = {
                eventTracker.logError(
                    extraMessage = "Error fetching initial payload",
                    error = it,
                    pane = Pane.INSTITUTION_PICKER,
                    logger = logger
                )
            }
        )
        onAsync(
            InstitutionPickerState::searchInstitutions,
            onFail = {
                eventTracker.logError(
                    extraMessage = "Error searching institutions",
                    error = it,
                    pane = Pane.INSTITUTION_PICKER,
                    logger = logger
                )
            }
        )
        onAsync(
            InstitutionPickerState::selectInstitution,
            onFail = {
                eventTracker.logError(
                    extraMessage = "Error selecting institution institutions",
                    error = it,
                    pane = Pane.INSTITUTION_PICKER,
                    logger = logger
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
                        pane = Pane.INSTITUTION_PICKER,
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
        }.execute {
            copy(searchInstitutions = if (it.isCancellationError()) Loading() else it)
        }
    }

    fun onInstitutionSelected(institution: FinancialConnectionsInstitution, fromFeatured: Boolean) {
        suspend {
            eventTracker.track(
                InstitutionSelected(
                    pane = Pane.INSTITUTION_PICKER,
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
        }.execute { async ->
            copy(
                selectedInstitutionId = institution.id.takeIf { async is Loading },
                selectInstitution = async
            )
        }
    }

    private fun navigateToPartnerAuth(authSession: FinancialConnectionsAuthorizationSession) {
        if (authSession.isOAuth) {
            navigationManager.tryNavigateTo(PartnerAuthDrawer(referrer = Pane.INSTITUTION_PICKER))
        } else {
            navigationManager.tryNavigateTo(PartnerAuth(referrer = Pane.INSTITUTION_PICKER))
        }
    }

    private fun navigateToPartnerAuth() {
        navigationManager.tryNavigateTo(PartnerAuth(referrer = Pane.INSTITUTION_PICKER))
    }

    fun onManualEntryClick() {
        navigationManager.tryNavigateTo(ManualEntry(referrer = Pane.INSTITUTION_PICKER))
    }

    fun onScrollChanged() {
        viewModelScope.launch {
            eventTracker.track(
                SearchScroll(
                    pane = Pane.INSTITUTION_PICKER,
                    institutionIds = awaitState().searchInstitutions()
                        ?.data
                        ?.map { it.id }
                        ?.toSet() ?: emptySet(),
                )
            )
        }
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
    val selectedInstitutionId: String? = null,
    val payload: Async<Payload> = Uninitialized,
    val searchInstitutions: Async<InstitutionResponse> = Uninitialized,
    val selectInstitution: Async<Unit> = Uninitialized
) : MavericksState {

    data class Payload(
        val featuredInstitutions: InstitutionResponse,
        val searchDisabled: Boolean,
        val featuredInstitutionsDuration: Long
    )
}
