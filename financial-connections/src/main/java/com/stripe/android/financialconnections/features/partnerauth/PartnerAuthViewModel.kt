package com.stripe.android.financialconnections.features.partnerauth

import android.webkit.URLUtil
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.AuthSessionEvent
import com.stripe.android.financialconnections.analytics.AuthSessionEvent.Launched
import com.stripe.android.financialconnections.analytics.AuthSessionEvent.Loaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.PrepaneClickContinue
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.di.APPLICATION_ID
import com.stripe.android.financialconnections.domain.CancelAuthorizationSession
import com.stripe.android.financialconnections.domain.CompleteAuthorizationSession
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.PollAuthorizationSessionOAuthResults
import com.stripe.android.financialconnections.domain.PostAuthSessionEvent
import com.stripe.android.financialconnections.domain.PostAuthorizationSession
import com.stripe.android.financialconnections.domain.RetrieveAuthorizationSession
import com.stripe.android.financialconnections.exception.WebAuthFlowFailedException
import com.stripe.android.financialconnections.features.common.enableCustomTabsService
import com.stripe.android.financialconnections.features.common.enableRetrieveAuthSession
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.Payload
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.ViewEffect.OpenPartnerAuth
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationDirections.accountPicker
import com.stripe.android.financialconnections.navigation.NavigationDirections.manualEntry
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.NavigationState.NavigateToRoute
import com.stripe.android.financialconnections.navigation.toNavigationCommand
import com.stripe.android.financialconnections.presentation.WebAuthFlowState
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.utils.UriUtils
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

@Suppress("LongParameterList")
internal class PartnerAuthViewModel @Inject constructor(
    private val completeAuthorizationSession: CompleteAuthorizationSession,
    private val createAuthorizationSession: PostAuthorizationSession,
    private val cancelAuthorizationSession: CancelAuthorizationSession,
    private val retrieveAuthorizationSession: RetrieveAuthorizationSession,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    @Named(APPLICATION_ID) private val applicationId: String,
    private val uriUtils: UriUtils,
    private val postAuthSessionEvent: PostAuthSessionEvent,
    private val getOrFetchSync: GetOrFetchSync,
    private val navigationManager: NavigationManager,
    private val pollAuthorizationSessionOAuthResults: PollAuthorizationSessionOAuthResults,
    private val logger: Logger,
    initialState: PartnerAuthState
) : MavericksViewModel<PartnerAuthState>(initialState) {

    init {
        logErrors()
        withState {
            if (it.activeAuthSession == null) {
                launchBrowserIfNonOauth()
                createAuthSession()
            } else {
                logger.debug("Restoring auth session ${it.activeAuthSession}")
                restoreAuthSession()
            }
        }
    }

    private fun restoreAuthSession() {
        suspend {
            // if coming from a process kill, there should be a session
            // re-fetch the manifest and use its active auth session instead of creating a new one
            val sync: SynchronizeSessionResponse = getOrFetchSync()
            val manifest: FinancialConnectionsSessionManifest = sync.manifest
            val authSession = manifest.activeAuthSession ?: createAuthorizationSession(
                institution = requireNotNull(manifest.activeInstitution),
                sync = sync
            )
            Payload(
                authSession = authSession,
                institution = requireNotNull(manifest.activeInstitution),
                isStripeDirect = manifest.isStripeDirect ?: false
            )
        }.execute { copy(payload = it) }
    }

    private fun createAuthSession() {
        suspend {
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
        }.execute {
            copy(
                payload = it,
                activeAuthSession = it()?.authSession?.id
            )
        }
    }

    private fun launchBrowserIfNonOauth() {
        onAsync(
            asyncProp = PartnerAuthState::payload,
            onSuccess = {
                // launch auth for non-OAuth (skip pre-pane).
                if (!it.authSession.isOAuth) launchAuthInBrowser()
            }
        )
    }

    private fun logErrors() {
        onAsync(
            PartnerAuthState::payload,
            onFail = {
                eventTracker.logError(
                    extraMessage = "Error fetching payload / posting AuthSession",
                    error = it,
                    logger = logger,
                    pane = Pane.PARTNER_AUTH
                )
            },
            onSuccess = { eventTracker.track(PaneLoaded(Pane.PARTNER_AUTH)) }
        )
    }

    fun onLaunchAuthClick() {
        viewModelScope.launch {
            awaitState().payload()?.authSession?.let {
                postAuthSessionEvent(it.id, AuthSessionEvent.OAuthLaunched(Date()))
                eventTracker.track(PrepaneClickContinue(Pane.PARTNER_AUTH))
            }
            launchAuthInBrowser()
        }
    }

    private suspend fun launchAuthInBrowser() = kotlin.runCatching {
        val manifest = requireNotNull(getOrFetchSync().manifest)
        val authSession = requireNotNull(manifest.activeAuthSession)
        val url = requireNotNull(
            authSession.url
                ?.replaceFirst("stripe-auth://native-redirect/$applicationId/", "")
        )
        setState {
            copy(
                viewEffect = OpenPartnerAuth(
                    url = url,
                    useCustomTabsService = manifest.enableCustomTabsService()
                )
            )
        }
    }.onFailure {
        eventTracker.logError(
            extraMessage = "failed retrieving active session from cache",
            error = it,
            logger = logger,
            pane = Pane.PARTNER_AUTH
        )
        setState { copy(authenticationStatus = Fail(it)) }
    }

    fun onSelectAnotherBank() {
        navigationManager.navigate(
            NavigateToRoute(
                command = NavigationDirections.reset,
                popCurrentFromBackStack = true
            )
        )
    }

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
                    setState { copy(authenticationStatus = Loading()) }
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
                FinancialConnectionsEvent.AuthSessionUrlReceived(
                    url = url,
                    authSessionId = authSession?.id,
                    status = "failed"
                )
            )
            eventTracker.logError(
                extraMessage = "Auth failed, cancelling AuthSession",
                error = error,
                logger = logger,
                pane = Pane.PARTNER_AUTH
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
                pane = Pane.PARTNER_AUTH
            )
        }
    }

    private suspend fun onAuthCancelled(url: String?) {
        kotlin.runCatching {
            logger.debug("Auth cancelled, cancelling AuthSession")
            setState { copy(authenticationStatus = Loading()) }
            val manifest = getOrFetchSync().manifest
            val authSession = manifest.activeAuthSession
            eventTracker.track(
                FinancialConnectionsEvent.AuthSessionUrlReceived(
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
                    FinancialConnectionsEvent.AuthSessionRetrieved(
                        authSessionId = retrievedAuthSession.id,
                        nextPane = nextPane
                    )
                )
                if (nextPane == Pane.PARTNER_AUTH) {
                    // auth session was not completed, proceed with cancellation
                    cancelAuthSessionAndContinue(authSession = retrievedAuthSession)
                } else {
                    // auth session succeeded although client didn't retrieve any deeplink.
                    postAuthSessionEvent(authSession.id, AuthSessionEvent.Success(Date()))
                    navigationManager.navigate(NavigateToRoute(nextPane.toNavigationCommand()))
                }
            } else {
                cancelAuthSessionAndContinue(authSession)
            }
        }.onFailure {
            eventTracker.logError(
                "failed cancelling session after cancelled web flow. url: $url",
                it,
                logger,
                Pane.PARTNER_AUTH
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
            createAuthSession()
        } else {
            // For non-OAuth institutions, navigate to Session cancellation's next pane.
            postAuthSessionEvent(authSession.id, AuthSessionEvent.Cancel(Date()))
            navigationManager.navigate(
                NavigateToRoute(
                    command = result.nextPane.toNavigationCommand(),
                    popCurrentFromBackStack = true
                )
            )
        }
    }

    private suspend fun completeAuthorizationSession(url: String) {
        kotlin.runCatching {
            setState { copy(authenticationStatus = Loading()) }
            val authSession = getOrFetchSync().manifest.activeAuthSession
            eventTracker.track(
                FinancialConnectionsEvent.AuthSessionUrlReceived(
                    url = url,
                    authSessionId = authSession?.id,
                    status = "success"
                )
            )
            requireNotNull(authSession)
            postAuthSessionEvent(authSession.id, AuthSessionEvent.Success(Date()))
            if (authSession.isOAuth) {
                logger.debug("Web AuthFlow completed! waiting for oauth results")
                val oAuthResults = pollAuthorizationSessionOAuthResults(authSession)
                logger.debug("OAuth results received! completing session")
                val updatedSession = completeAuthorizationSession(
                    authorizationSessionId = authSession.id,
                    publicToken = oAuthResults.publicToken
                )
                logger.debug("Session authorized!")
                navigationManager.navigate(
                    NavigateToRoute(
                        command = updatedSession.nextPane.toNavigationCommand(),
                        popCurrentFromBackStack = true
                    )
                )
            } else {
                navigationManager.navigate(
                    NavigateToRoute(
                        command = accountPicker,
                        popCurrentFromBackStack = true
                    )
                )
            }
        }.onFailure {
            eventTracker.logError(
                extraMessage = "failed authorizing session",
                error = it,
                logger = logger,
                pane = Pane.PARTNER_AUTH
            )
            setState { copy(authenticationStatus = Fail(it)) }
        }
    }

    fun onEnterDetailsManuallyClick() = navigationManager.navigate(
        NavigateToRoute(
            command = manualEntry,
            popCurrentFromBackStack = true
        )
    )

    // if clicked uri contains an eventName query param, track click event.
    fun onClickableTextClick(uri: String) = viewModelScope.launch {
        uriUtils.getQueryParameter(uri, "eventName")?.let { eventName ->
            eventTracker.track(
                FinancialConnectionsEvent.Click(
                    eventName,
                    pane = Pane.PARTNER_AUTH
                )
            )
        }
        if (URLUtil.isNetworkUrl(uri)) {
            setState {
                copy(
                    viewEffect = OpenUrl(
                        uri,
                        Date().time
                    )
                )
            }
        } else {
            val managedUri = PartnerAuthState.ClickableText.values()
                .firstOrNull { uriUtils.compareSchemeAuthorityAndPath(it.value, uri) }
            when (managedUri) {
                PartnerAuthState.ClickableText.DATA -> {
                    setState {
                        copy(
                            viewEffect = PartnerAuthState.ViewEffect.OpenBottomSheet(Date().time)
                        )
                    }
                }

                null -> logger.error("Unrecognized clickable text: $uri")
            }
        }
    }

    fun onViewEffectLaunched() {
        setState {
            copy(viewEffect = null)
        }
    }

    companion object : MavericksViewModelFactory<PartnerAuthViewModel, PartnerAuthState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: PartnerAuthState
        ): PartnerAuthViewModel {
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .partnerAuthSubcomponent
                .initialState(state)
                .build()
                .viewModel
        }
    }
}
