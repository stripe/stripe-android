package com.stripe.android.financialconnections.features.streamlinedconsent

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.Click
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.AcceptConsent
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.HandleError
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.PostAuthorizationSession
import com.stripe.android.financialconnections.features.consent.consentDeeplinkActions
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.DataAccess
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.Legal
import com.stripe.android.financialconnections.features.notice.PresentSheet
import com.stripe.android.financialconnections.features.streamlinedconsent.StreamlinedConsentState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.ui.HandleClickableUrl
import com.stripe.android.financialconnections.utils.error
import com.stripe.android.uicore.navigation.NavigationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

internal class StreamlinedConsentViewModel @AssistedInject constructor(
    @Assisted initialState: StreamlinedConsentState,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val acceptConsent: AcceptConsent,
    private val getOrFetchSync: GetOrFetchSync,
    private val navigationManager: NavigationManager,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val handleClickableUrl: HandleClickableUrl,
    private val presentSheet: PresentSheet,
    private val postAuthorizationSession: PostAuthorizationSession,
    private val handleError: HandleError,
    private val logger: Logger,
) : FinancialConnectionsViewModel<StreamlinedConsentState>(initialState, nativeAuthFlowCoordinator) {

    init {
        suspend {
            val sync = getOrFetchSync()
            val streamlinedConsentPane = requireNotNull(sync.text?.streamlinedConsentPane) {
                "Streamlined consent pane is missing"
            }
            StreamlinedConsentState.Payload(
                streamlinedConsentPane = streamlinedConsentPane,
                // Like web and the account pickers, prefer the regular consent pane's notice so that
                // the same notice is shown throughout the session.
                dataAccessNotice = sync.text?.consent?.dataAccessNotice
                    ?: streamlinedConsentPane.dataAccessNotice,
            )
        }.execute { copy(payload = it) }

        onAsync(StreamlinedConsentState::payload, onSuccess = {
            eventTracker.track(PaneLoaded(PANE))
        })

        onAsync(StreamlinedConsentState::acceptConsent, onFail = {
            eventTracker.logError(
                extraMessage = "Error accepting consent",
                error = it,
                logger = logger,
                pane = PANE,
            )
        })
    }

    override fun updateTopAppBar(state: StreamlinedConsentState): TopAppBarStateUpdate {
        return TopAppBarStateUpdate(
            pane = PANE,
            hideStripeLogo = false,
            // The institution picker remains on the back stack, so users can choose another institution.
            allowBackNavigation = true,
            error = state.payload.error,
            canCloseWithoutConfirmation = true,
            logoClickNotice = state.payload()?.streamlinedConsentPane?.moreInfoNotice,
        )
    }

    fun onContinueClick() {
        suspend {
            eventTracker.track(Click(eventName = "click.agree", pane = PANE))
            val updatedManifest: FinancialConnectionsSessionManifest = acceptConsent()
            eventTracker.emitEvent(Name.CONSENT_ACQUIRED)
            navigateToNextPane(updatedManifest)
            updatedManifest
        }.execute { copy(acceptConsent = it) }
    }

    private suspend fun navigateToNextPane(manifest: FinancialConnectionsSessionManifest) {
        if (manifest.nextPane != Pane.PARTNER_AUTH) {
            navigationManager.tryNavigateTo(manifest.nextPane.destination(referrer = PANE))
            return
        }

        // Like the institution picker, create the auth session up front so that we know whether to
        // show the OAuth prepane as a drawer.
        val sync = getOrFetchSync()
        val authSession = runCatching {
            postAuthorizationSession(
                institution = requireNotNull(sync.manifest.activeInstitution),
                sync = sync,
            )
        }.getOrElse { error ->
            handleError(
                extraMessage = "Error creating session for institution",
                error = error,
                pane = PANE,
                displayErrorScreen = true,
            )
            return
        }

        navigationManager.tryNavigateTo(
            if (authSession.isOAuth) {
                Destination.PartnerAuthDrawer(referrer = PANE)
            } else {
                Destination.PartnerAuth(referrer = PANE)
            }
        )
    }

    fun onClickableTextClick(uri: String) {
        viewModelScope.launch {
            handleClickableUrl(
                currentPane = PANE,
                uri = uri,
                onNetworkUrlClicked = { setState { copy(viewEffect = OpenUrl(uri)) } },
                knownDeeplinkActions = consentDeeplinkActions(
                    pane = PANE,
                    navigationManager = navigationManager,
                    onDataAccessClick = ::presentDataAccessBottomSheet,
                    onLegalDetailsClick = ::presentLegalDetailsBottomSheet,
                ),
            )
        }
    }

    fun onViewEffectLaunched() {
        setState { copy(viewEffect = null) }
    }

    private fun presentDataAccessBottomSheet() {
        withPayload {
            val dataAccessNotice = it.dataAccessNotice
            if (dataAccessNotice == null) {
                eventTracker.logError(
                    extraMessage = "Data access notice is missing",
                    error = IllegalStateException("Data access notice is missing"),
                    logger = logger,
                    pane = PANE,
                )
                return@withPayload
            }
            presentSheet(
                content = DataAccess(dataAccessNotice),
                referrer = PANE,
            )
        }
    }

    private fun presentLegalDetailsBottomSheet() {
        withPayload {
            presentSheet(
                content = Legal(it.streamlinedConsentPane.legalDetailsNotice),
                referrer = PANE,
            )
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(initialState: StreamlinedConsentState): StreamlinedConsentViewModel
    }

    companion object {

        internal val PANE = Pane.STREAMLINED_CONSENT

        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    parentComponent.streamlinedConsentViewModelFactory.create(StreamlinedConsentState())
                }
            }
    }
}

private fun StreamlinedConsentViewModel.withPayload(
    block: (StreamlinedConsentState.Payload) -> Unit,
) {
    val payload = stateFlow.value.payload() ?: return
    block(payload)
}
