package com.stripe.android.financialconnections.features.partnerauth

import android.webkit.URLUtil
import androidx.core.net.toUri
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
import com.stripe.android.financialconnections.browser.BrowserManager
import com.stripe.android.financialconnections.di.APPLICATION_ID
import com.stripe.android.financialconnections.domain.AuthSessionChallengeResponse
import com.stripe.android.financialconnections.domain.CancelAuthorizationSession
import com.stripe.android.financialconnections.domain.ChallengeFlowController
import com.stripe.android.financialconnections.domain.CompleteAuthorizationSession
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.PollAuthorizationSessionOAuthResults
import com.stripe.android.financialconnections.domain.PostAuthSessionEvent
import com.stripe.android.financialconnections.domain.PostAuthorizationSession
import com.stripe.android.financialconnections.domain.RetrieveAuthorizationSession
import com.stripe.android.financialconnections.exception.WebAuthFlowFailedException
import com.stripe.android.financialconnections.features.common.enableRetrieveAuthSession
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.Payload
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.ViewEffect.OpenPartnerAuth
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.navigation.Destination.AccountPicker
import com.stripe.android.financialconnections.navigation.Destination.ManualEntry
import com.stripe.android.financialconnections.navigation.Destination.Reset
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.destination
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
    private val challengeFlowController: ChallengeFlowController,
    private val getOrFetchSync: GetOrFetchSync,
    private val browserManager: BrowserManager,
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
            buildPayload(authSession, manifest, null)//TODO figure.
        }.execute { copy(payload = it) }
    }

    private fun isChallengeFlow(flow: String): Boolean {
        // we currently have two different mx challenge-flow flows
        return flow.matches(Regex("^(mx_nonoauth|mx_non_oauth|independent)"))
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
            val challenge = if (isChallengeFlow(authSession.flow!!)) {
                challengeFlowController.getChallenge(authSession.id)
            } else null

            buildPayload(authSession, manifest, challenge).also {
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

    private fun buildPayload(
        authSession: FinancialConnectionsAuthorizationSession,
        manifest: FinancialConnectionsSessionManifest,
        challenge: AuthSessionChallengeResponse?
    ) = Payload(
        authSession = authSession,
        institution = requireNotNull(manifest.activeInstitution),
        isStripeDirect = manifest.isStripeDirect ?: false,
        isChallenge = isChallengeFlow(authSession.flow!!),
        repairPayload = null,
        challengePayload = challenge?.let {
            PartnerAuthState.ChallengePayload(
                id = requireNotNull(it.challenge).id,
                type = requireNotNull(it.challenge).type
            )
        }
    )

    private fun launchBrowserIfNonOauth() {
        onAsync(
            asyncProp = PartnerAuthState::payload,
            onSuccess = {
                // launch auth for non-OAuth (skip pre-pane).
                when {
                    it.authSession.isOAuth.not() && it.isChallenge.not() -> launchAuthInBrowser()
                }
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
                    pane = PANE
                )
            },
            onSuccess = { eventTracker.track(PaneLoaded(PANE)) }
        )
    }

    fun onLaunchAuthClick() {
        viewModelScope.launch {
            awaitState().payload()?.let {
                postAuthSessionEvent(it.authSession.id, AuthSessionEvent.OAuthLaunched(Date()))
                eventTracker.track(PrepaneClickContinue(PANE))
                launchAuthInBrowser()
            }
        }
    }

    private suspend fun launchAuthInBrowser() = runCatching {
        val authSession = requireNotNull(awaitState().payload()).authSession
        val url = requireNotNull(authSession.browserReadyUrl(applicationId))
        setState { copy(viewEffect = OpenPartnerAuth(url)) }
        eventTracker.track(
            FinancialConnectionsEvent.AuthSessionOpened(
                id = authSession.id,
                pane = PANE,
                flow = authSession.flow,
                defaultBrowser = browserManager.getPackageToHandleUri(
                    uri = url.toUri()
                )
            )
        )
    }

    fun onSelectAnotherBank() {
        navigationManager.tryNavigateTo(
            Reset(referrer = PANE),
            popUpToCurrent = true,
            inclusive = true
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
                pane = PANE
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
                pane = PANE
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
                if (nextPane == PANE) {
                    // auth session was not completed, proceed with cancellation
                    cancelAuthSessionAndContinue(authSession = retrievedAuthSession)
                } else {
                    // auth session succeeded although client didn't retrieve any deeplink.
                    postAuthSessionEvent(authSession.id, AuthSessionEvent.Success(Date()))
                    navigationManager.tryNavigateTo(nextPane.destination(referrer = PANE))
                }
            } else {
                cancelAuthSessionAndContinue(authSession)
            }
        }.onFailure {
            eventTracker.logError(
                "failed cancelling session after cancelled web flow. url: $url",
                it,
                logger,
                PANE
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
            navigationManager.tryNavigateTo(
                result.nextPane.destination(referrer = PANE),
                popUpToCurrent = true,
                inclusive = true
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
                navigationManager.tryNavigateTo(
                    updatedSession.nextPane.destination(referrer = PANE),
                    popUpToCurrent = true,
                    inclusive = true
                )
            } else {
                navigationManager.tryNavigateTo(
                    AccountPicker(referrer = PANE),
                    popUpToCurrent = true,
                    inclusive = true
                )
            }
        }.onFailure {
            eventTracker.logError(
                extraMessage = "failed authorizing session",
                error = it,
                logger = logger,
                pane = PANE
            )
            setState { copy(authenticationStatus = Fail(it)) }
        }
    }

    fun onEnterDetailsManuallyClick() = navigationManager.tryNavigateTo(
        ManualEntry(referrer = PANE),
        popUpToCurrent = true,
        inclusive = true
    )

    // if clicked uri contains an eventName query param, track click event.
    fun onClickableTextClick(uri: String) = viewModelScope.launch {
        uriUtils.getQueryParameter(uri, "eventName")?.let { eventName ->
            eventTracker.track(
                FinancialConnectionsEvent.Click(
                    eventName,
                    pane = PANE
                )
            )
        }
        if (URLUtil.isNetworkUrl(uri)) {
            setState {
                copy(
                    viewEffect = PartnerAuthState.ViewEffect.OpenUrl(
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

    fun onFormSubmit(username: String, password: String) {
        viewModelScope.launch {
            awaitState().payload()?.let {
                val challenge = it.challengePayload!!
                challengeFlowController.submitChallenge(
                    authSessionId = it.authSession.id,
                    username = username,
                    password = password,
                    challengeId = challenge.id,
                    type = challenge.type
                )
                navigationManager.tryNavigateTo(
                    AccountPicker(referrer = PANE),
                    popUpToCurrent = true,
                    inclusive = true
                )
            }
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

        private val PANE = Pane.PARTNER_AUTH
    }
}
