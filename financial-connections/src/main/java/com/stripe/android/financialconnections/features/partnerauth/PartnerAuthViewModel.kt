package com.stripe.android.financialconnections.features.partnerauth

import android.os.Parcelable
import android.webkit.URLUtil
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.FinancialConnections
import com.stripe.android.financialconnections.analytics.AuthSessionEvent
import com.stripe.android.financialconnections.analytics.AuthSessionEvent.Launched
import com.stripe.android.financialconnections.analytics.AuthSessionEvent.Loaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.AuthSessionOpened
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.AuthSessionRetrieved
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.AuthSessionUrlReceived
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.Click
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PrepaneClickCancel
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PrepaneClickChooseAnotherBank
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PrepaneClickContinue
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Name
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.browser.BrowserManager
import com.stripe.android.financialconnections.di.APPLICATION_ID
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.CancelAuthorizationSession
import com.stripe.android.financialconnections.domain.CompleteAuthorizationSession
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.GetOrFetchSync.RefetchCondition.IfMissingActiveAuthSession
import com.stripe.android.financialconnections.domain.HandleError
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.PollAuthorizationSessionOAuthResults
import com.stripe.android.financialconnections.domain.PostAuthSessionEvent
import com.stripe.android.financialconnections.domain.PostAuthorizationSession
import com.stripe.android.financialconnections.domain.RepairAuthorizationSession
import com.stripe.android.financialconnections.domain.RetrieveAuthorizationSession
import com.stripe.android.financialconnections.exception.FinancialConnectionsError
import com.stripe.android.financialconnections.exception.PartnerAuthError
import com.stripe.android.financialconnections.exception.WebAuthFlowFailedException
import com.stripe.android.financialconnections.features.common.enableRetrieveAuthSession
import com.stripe.android.financialconnections.features.notice.NoticeSheetState.NoticeSheetContent.DataAccess
import com.stripe.android.financialconnections.features.notice.PresentSheet
import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthState.AuthenticationStatus.Action
import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthState.Payload
import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthState.ViewEffect
import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthState.ViewEffect.OpenPartnerAuth
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.Destination.AccountPicker
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.PopUpToBehavior
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.Async.Fail
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.presentation.WebAuthFlowState
import com.stripe.android.financialconnections.repository.CoreAuthorizationPendingNetworkingRepairRepository
import com.stripe.android.financialconnections.utils.UriUtils
import com.stripe.android.financialconnections.utils.error
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.util.Date
import javax.inject.Named
import com.stripe.android.financialconnections.features.partnerauth.SharedPartnerAuthState.AuthenticationStatus as Status

internal class PartnerAuthViewModel @AssistedInject constructor(
    private val completeAuthorizationSession: CompleteAuthorizationSession,
    private val createAuthorizationSession: PostAuthorizationSession,
    private val cancelAuthorizationSession: CancelAuthorizationSession,
    private val retrieveAuthorizationSession: RetrieveAuthorizationSession,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    @Named(APPLICATION_ID) private val applicationId: String,
    private val uriUtils: UriUtils,
    private val postAuthSessionEvent: PostAuthSessionEvent,
    private val getOrFetchSync: GetOrFetchSync,
    private val browserManager: BrowserManager,
    private val handleError: HandleError,
    private val navigationManager: NavigationManager,
    private val pollAuthorizationSessionOAuthResults: PollAuthorizationSessionOAuthResults,
    private val logger: Logger,
    private val presentSheet: PresentSheet,
    private val pendingRepairRepository: CoreAuthorizationPendingNetworkingRepairRepository,
    private val repairAuthSession: RepairAuthorizationSession,
    @Assisted private val initialState: SharedPartnerAuthState,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
) : FinancialConnectionsViewModel<SharedPartnerAuthState>(initialState, nativeAuthFlowCoordinator) {

    private val pane: Pane
        get() = initialState.pane

    init {
        handleErrors()
        launchBrowserIfNonOauth()
        initializeState()
    }

    override fun updateTopAppBar(state: SharedPartnerAuthState): TopAppBarStateUpdate? {
        return if (state.inModal) {
            null
        } else {
            TopAppBarStateUpdate(
                pane = state.pane,
                allowBackNavigation = state.canNavigateBack,
                error = state.payload.error,
            )
        }
    }

    private fun initializeState() {
        suspend {
            val sync = getOrFetchSync()
            if (pane == Pane.BANK_AUTH_REPAIR) {
                initializeBankAuthRepair(sync)
            } else {
                initializePartnerAuth(sync)
            }
        }.execute {
            copy(payload = it)
        }
    }

    private suspend fun initializeBankAuthRepair(
        sync: SynchronizeSessionResponse,
    ): Payload {
        val authorization = requireNotNull(pendingRepairRepository.get()?.coreAuthorization)
        val activeInstitution = requireNotNull(sync.manifest.activeInstitution)

        val authSession = repairAuthSession(authorization)

        return Payload(
            isStripeDirect = sync.manifest.isStripeDirect ?: false,
            institution = activeInstitution,
            authSession = authSession,
        )
    }

    private suspend fun initializePartnerAuth(
        sync: SynchronizeSessionResponse,
    ): Payload {
        val manifest = sync.manifest
        val authSession = manifest.activeAuthSession ?: createAuthorizationSession(
            institution = requireNotNull(manifest.activeInstitution),
            sync = sync
        )
        return Payload(
            isStripeDirect = manifest.isStripeDirect ?: false,
            institution = requireNotNull(manifest.activeInstitution),
            authSession = authSession,
        )
    }

    private fun recreateAuthSession() = suspend {
        val launchedEvent = Launched(Date())
        val sync: SynchronizeSessionResponse = getOrFetchSync()
        val manifest: FinancialConnectionsSessionManifest = sync.manifest
        val authSession = createAuthorizationSession(
            institution = requireNotNull(manifest.activeInstitution),
            sync = sync
        )
        logger.debug("Created auth session ${authSession.id}")
        Payload(
            authSession = authSession,
            institution = requireNotNull(manifest.activeInstitution),
            isStripeDirect = manifest.isStripeDirect ?: false
        ).also {
            // just send loaded event on OAuth flows (prepane). Non-OAuth handled by shim.
            val loadedEvent: Loaded? = Loaded(Date()).takeIf { authSession.isOAuth }
            postAuthSessionEvent(
                authSession.id,
                listOfNotNull(launchedEvent, loadedEvent)
            )
        }
    }.execute(
        // keeps existing payload to prevent showing full-screen loading.
        retainValue = SharedPartnerAuthState::payload
    ) {
        copy(payload = it)
    }

    private fun launchBrowserIfNonOauth() {
        onAsync(
            prop = SharedPartnerAuthState::payload,
            onSuccess = {
                // launch auth for non-OAuth (skip pre-pane).
                if (!it.authSession.isOAuth) {
                    launchAuthInBrowser(it.authSession)
                }
            }
        )
    }

    private fun handleErrors() {
        onAsync(
            SharedPartnerAuthState::payload,
            onFail = {
                handleError(
                    extraMessage = "Error fetching payload / posting AuthSession",
                    error = it,
                    pane = pane,
                    displayErrorScreen = true
                )
            },
            onSuccess = { eventTracker.track(PaneLoaded(pane)) }
        )
        onAsync(
            SharedPartnerAuthState::authenticationStatus,
            onFail = {
                handleError(
                    extraMessage = "Error with authentication status",
                    error = if (it is FinancialConnectionsError) it else PartnerAuthError(it.message),
                    pane = pane,
                    displayErrorScreen = true
                )
            }
        )
    }

    fun onLaunchAuthClick() {
        setState { copy(authenticationStatus = Loading(value = Status(Action.AUTHENTICATING))) }

        withState { state ->
            val authSession = requireNotNull(state.payload()?.authSession) {
                "Payload shouldn't be null when the user launches the auth flow"
            }

            reportOAuthLaunched(authSession.id)
            launchAuthInBrowser(authSession)
        }
    }

    private fun reportOAuthLaunched(sessionId: String) {
        postAuthSessionEvent(sessionId, AuthSessionEvent.OAuthLaunched(Date()))
        eventTracker.track(PrepaneClickContinue(pane))
    }

    private fun launchAuthInBrowser(authSession: FinancialConnectionsAuthorizationSession) {
        authSession.browserReadyUrl()?.let { url ->
            setState { copy(viewEffect = OpenPartnerAuth(url)) }
            eventTracker.track(
                AuthSessionOpened(
                    id = authSession.id,
                    pane = pane,
                    flow = authSession.flow,
                    defaultBrowser = browserManager.getPackageToHandleUri(uri = url.toUri()),
                )
            )
        }
    }

    /**
     * Auth Session url after clearing the deep link prefix (required for non-native app2app flows).
     */
    private fun FinancialConnectionsAuthorizationSession.browserReadyUrl(): String? =
        url?.replaceFirst("stripe-auth://native-redirect/$applicationId/", "")

    fun onWebAuthFlowFinished(
        webStatus: WebAuthFlowState
    ) {
        logger.debug("Web AuthFlow status received $webStatus")
        viewModelScope.launch {
            when (webStatus) {
                is WebAuthFlowState.Canceled -> {
                    onAuthCancelled(webStatus.url)
                }

                is WebAuthFlowState.Failed -> {
                    onAuthFailed(webStatus.url, webStatus.message, webStatus.reason)
                }

                WebAuthFlowState.InProgress -> {
                    setState {
                        copy(
                            authenticationStatus = Loading(Status(Action.AUTHENTICATING))
                        )
                    }
                }

                is WebAuthFlowState.Success -> {
                    completeAuthorizationSession(webStatus.url)
                }

                WebAuthFlowState.Uninitialized -> {}
            }
        }
    }

    private suspend fun onAuthFailed(
        url: String,
        message: String,
        reason: String?
    ) {
        val error = WebAuthFlowFailedException(message, reason)
        kotlin.runCatching {
            val authSession = getOrFetchSync().manifest.activeAuthSession
            eventTracker.track(
                AuthSessionUrlReceived(
                    pane = pane,
                    url = url,
                    authSessionId = authSession?.id,
                    status = "failed"
                )
            )
            eventTracker.logError(
                extraMessage = "Auth failed, cancelling AuthSession",
                error = error,
                logger = logger,
                pane = pane
            )
            when {
                authSession != null -> {
                    postAuthSessionEvent(authSession.id, AuthSessionEvent.Failure(Date(), error))
                    cancelAuthorizationSession(authSession.id)
                }

                else -> logger.debug("Could not find AuthSession to cancel.")
            }
            setState { copy(authenticationStatus = Fail(error)) }
        }.onFailure {
            eventTracker.logError(
                extraMessage = "failed cancelling session after failed web flow",
                error = it,
                logger = logger,
                pane = pane
            )
        }
    }

    private suspend fun onAuthCancelled(url: String?) {
        kotlin.runCatching {
            logger.debug("Auth cancelled, cancelling AuthSession")
            setState { copy(authenticationStatus = Loading(value = Status(Action.AUTHENTICATING))) }
            val manifest = getOrFetchSync(
                refetchCondition = IfMissingActiveAuthSession
            ).manifest
            val authSession = manifest.activeAuthSession
            eventTracker.track(
                AuthSessionUrlReceived(
                    pane = pane,
                    url = url ?: "none",
                    authSessionId = authSession?.id,
                    status = "cancelled"
                )
            )
            requireNotNull(authSession)
            if (manifest.enableRetrieveAuthSession()) {
                // if the client canceled mid-flow (either by closing the browser or
                // cancelling on the institution page), retrieve the auth session
                // and try to recover if possible.
                val retrievedAuthSession = retrieveAuthorizationSession(authSession.id)
                val nextPane = retrievedAuthSession.nextPane
                eventTracker.track(
                    AuthSessionRetrieved(
                        authSessionId = retrievedAuthSession.id,
                        nextPane = nextPane
                    )
                )
                if (nextPane == pane) {
                    // auth session was not completed, proceed with cancellation
                    cancelAuthSessionAndContinue(authSession = retrievedAuthSession)
                } else {
                    // auth session succeeded although client didn't retrieve any deeplink.
                    postAuthSessionEvent(authSession.id, AuthSessionEvent.Success(Date()))
                    navigationManager.tryNavigateTo(nextPane.destination(referrer = pane))
                }
            } else {
                cancelAuthSessionAndContinue(authSession)
            }
        }.onFailure {
            eventTracker.logError(
                "failed cancelling session after cancelled web flow. url: $url",
                it,
                logger,
                pane
            )
            setState { copy(authenticationStatus = Fail(it)) }
        }
    }

    /**
     * Cancels the given [authSession] and navigates to the next pane (non-OAuth) / retries (OAuth).
     */
    private suspend fun cancelAuthSessionAndContinue(
        authSession: FinancialConnectionsAuthorizationSession
    ) {
        val result = cancelAuthorizationSession(authSession.id)
        if (authSession.isOAuth) {
            // For OAuth institutions, create a new session and navigate to its nextPane (prepane).
            logger.debug("Creating a new session for this OAuth institution")
            // Send retry event as we're presenting the prepane again.
            postAuthSessionEvent(authSession.id, AuthSessionEvent.Retry(Date()))
            // for OAuth institutions, we remain on the pre-pane,
            // but create a brand new auth session
            setState { copy(authenticationStatus = Uninitialized) }
            recreateAuthSession()
        } else {
            // For non-OAuth institutions, navigate to Session cancellation's next pane.
            postAuthSessionEvent(authSession.id, AuthSessionEvent.Cancel(Date()))
            navigationManager.tryNavigateTo(
                route = result.nextPane.destination(referrer = pane),
                popUpTo = PopUpToBehavior.Current(inclusive = true),
            )
        }
    }

    private suspend fun completeAuthorizationSession(url: String) {
        kotlin.runCatching {
            setState { copy(authenticationStatus = Loading(value = Status(Action.AUTHENTICATING))) }
            val authSession = getOrFetchSync(
                refetchCondition = IfMissingActiveAuthSession
            ).manifest.activeAuthSession
            eventTracker.track(
                AuthSessionUrlReceived(
                    pane = pane,
                    url = url,
                    authSessionId = authSession?.id,
                    status = "success"
                )
            )
            requireNotNull(authSession)
            postAuthSessionEvent(authSession.id, AuthSessionEvent.Success(Date()))
            val nextPane = if (authSession.isOAuth) {
                logger.debug("Web AuthFlow completed! waiting for oauth results")
                val oAuthResults = pollAuthorizationSessionOAuthResults(authSession)
                logger.debug("OAuth results received! completing session")
                val updatedSession = completeAuthorizationSession(
                    authorizationSessionId = authSession.id,
                    publicToken = oAuthResults.publicToken
                )
                logger.debug("Session authorized!")
                updatedSession.nextPane.destination(referrer = pane)
            } else {
                AccountPicker(referrer = pane)
            }
            FinancialConnections.emitEvent(Name.INSTITUTION_AUTHORIZED)
            navigationManager.tryNavigateTo(nextPane)
        }.onFailure {
            eventTracker.logError(
                extraMessage = "failed authorizing session",
                error = it,
                logger = logger,
                pane = pane
            )
            setState { copy(authenticationStatus = Fail(it)) }
        }
    }

    // if clicked uri contains an eventName query param, track click event.
    fun onClickableTextClick(uri: String) = viewModelScope.launch {
        uriUtils.getQueryParameter(uri, "eventName")?.let { eventName ->
            eventTracker.track(Click(eventName, pane = pane))
        }
        if (URLUtil.isNetworkUrl(uri)) {
            setState {
                copy(
                    viewEffect = ViewEffect.OpenUrl(
                        uri,
                        Date().time
                    )
                )
            }
        } else {
            val managedUri = SharedPartnerAuthState.ClickableText.entries
                .firstOrNull { uriUtils.compareSchemeAuthorityAndPath(it.value, uri) }
            when (managedUri) {
                SharedPartnerAuthState.ClickableText.DATA -> presentDataAccessBottomSheet()
                null -> logger.error("Unrecognized clickable text: $uri")
            }
        }
    }

    private fun presentDataAccessBottomSheet() {
        val authSession = stateFlow.value.payload()?.authSession
        val notice = authSession?.display?.text?.consent?.dataAccessNotice ?: return
        presentSheet(
            content = DataAccess(notice),
            referrer = pane,
        )
    }

    fun onViewEffectLaunched() {
        setState {
            copy(viewEffect = null)
        }
    }

    fun onCancelClick() = withState { state ->
        if (state.inModal) {
            eventTracker.track(PrepaneClickCancel(pane = pane))
        } else {
            eventTracker.track(PrepaneClickChooseAnotherBank(pane = pane))
        }

        viewModelScope.launch {
            // set loading state while cancelling the active auth session, and navigate back
            setState { copy(authenticationStatus = Loading(value = Status(Action.CANCELLING))) }
            runCatching {
                val authSession = requireNotNull(
                    getOrFetchSync(refetchCondition = IfMissingActiveAuthSession).manifest.activeAuthSession
                )
                cancelAuthorizationSession(authSession.id)
            }
            if (state.inModal) {
                cancelInModal()
            } else {
                cancelInFullscreen()
            }
        }
    }

    private fun cancelInModal() {
        navigationManager.tryNavigateBack()
    }

    private fun cancelInFullscreen() {
        navigationManager.tryNavigateTo(
            route = Destination.InstitutionPicker(referrer = pane),
            popUpTo = PopUpToBehavior.Current(
                inclusive = true,
            ),
        )
    }

    @Parcelize
    data class Args(val inModal: Boolean, val pane: Pane) : Parcelable

    @AssistedFactory
    interface Factory {
        fun create(initialState: SharedPartnerAuthState): PartnerAuthViewModel
    }

    companion object {
        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent, args: Args): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    parentComponent.partnerAuthViewModelFactory.create(SharedPartnerAuthState(args))
                }
            }
    }
}
