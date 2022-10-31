package com.stripe.android.financialconnections.features.partnerauth

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
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.PaneLoaded
import com.stripe.android.financialconnections.domain.CancelAuthorizationSession
import com.stripe.android.financialconnections.domain.CompleteAuthorizationSession
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.domain.PollAuthorizationSessionOAuthResults
import com.stripe.android.financialconnections.domain.PostAuthorizationSession
import com.stripe.android.financialconnections.exception.WebAuthFlowCancelledException
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.PartnerAuthViewEffect.OpenPartnerAuth
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession.Flow
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.NextPane
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
internal class PartnerAuthViewModel @Inject constructor(
    private val completeAuthorizationSession: CompleteAuthorizationSession,
    private val createAuthorizationSession: PostAuthorizationSession,
    private val cancelAuthorizationSession: CancelAuthorizationSession,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val getManifest: GetManifest,
    private val goNext: GoNext,
    private val navigationManager: NavigationManager,
    private val pollAuthorizationSessionOAuthResults: PollAuthorizationSessionOAuthResults,
    private val logger: Logger,
    initialState: PartnerAuthState
) : MavericksViewModel<PartnerAuthState>(initialState) {
    init {
        logErrors()
        suspend {
            /**
             * if viewModel dies while WebAuth happens, avoid creating a new auth session.
             * [FinancialConnectionsSessionManifest] gets re-fetched, and the existing
             * [FinancialConnectionsSessionManifest.activeAuthSession] will be used instead.
             */
            val manifest: FinancialConnectionsSessionManifest = getManifest()
            val authSession = createAuthorizationSession(
                institution = requireNotNull(manifest.activeInstitution),
                allowManualEntry = manifest.allowManualEntry
            ).also { launchAuthIfSkipPrepane() }
            Payload(
                flow = authSession.flow,
                showPrepane = authSession.isOAuth,
                showPartnerDisclosure = authSession.showPartnerDisclosure ?: false,
                institution = requireNotNull(manifest.activeInstitution)
            )
        }.execute {
            copy(payload = it)
        }
    }

    private fun launchAuthIfSkipPrepane() {
        onAsync(
            asyncProp = PartnerAuthState::payload,
            onSuccess = { if (it.showPrepane.not()) onLaunchAuthClick() }
        )
    }

    private fun logErrors() {
        onAsync(
            PartnerAuthState::payload,
            onFail = {
                logger.error("Error fetching payload / posting AuthSession", it)
                eventTracker.track(FinancialConnectionsEvent.Error(it))
            },
            onSuccess = { eventTracker.track(PaneLoaded(NextPane.PARTNER_AUTH)) }
        )
    }

    fun onLaunchAuthClick() {
        viewModelScope.launch {
            kotlin.runCatching { requireNotNull(getManifest().activeAuthSession) }
                .onSuccess {
                    it.url?.let { setState { copy(viewEffect = OpenPartnerAuth(it)) } }
                }
                .onFailure {
                    eventTracker.track(FinancialConnectionsEvent.Error(it))
                    logger.error("failed retrieving active session from cache", it)
                    setState { copy(authenticationStatus = Fail(it)) }
                }
        }
    }

    fun onSelectAnotherBank() {
        navigationManager.navigate(NavigationDirections.reset)
    }

    fun onWebAuthFlowFinished(
        webStatus: Async<String>
    ) {
        logger.debug("Web AuthFlow status received $webStatus")
        viewModelScope.launch {
            when (webStatus) {
                is Uninitialized -> {}
                is Loading -> setState { copy(authenticationStatus = Loading()) }
                is Success -> completeAuthorizationSession()
                is Fail -> {
                    when (val error = webStatus.error) {
                        is WebAuthFlowCancelledException -> onAuthCancelled()
                        else -> onAuthFailed(error)
                    }
                }
            }
        }
    }

    private suspend fun onAuthFailed(
        error: Throwable
    ) {
        kotlin.runCatching {
            logger.debug("Auth failed, cancelling AuthSession")
            val authSession = getManifest().activeAuthSession
            logger.error("Auth failed, cancelling AuthSession", error)
            when {
                authSession != null -> cancelAuthorizationSession(authSession.id)
                else -> logger.debug("Could not find AuthSession to cancel.")
            }
            setState { copy(authenticationStatus = Fail(error)) }
        }.onFailure {
            logger.error("failed cancelling session after failed web flow", it)
        }
    }

    private suspend fun onAuthCancelled() {
        kotlin.runCatching {
            logger.debug("Auth cancelled, cancelling AuthSession")
            setState { copy(authenticationStatus = Loading()) }
            val authSession = requireNotNull(getManifest().activeAuthSession)
            val result = cancelAuthorizationSession(authSession.id)
            if (authSession.isOAuth) {
                // For OAuth institutions, create a new session and navigate to its nextPane.
                logger.debug("Creating a new session for this OAuth institution")
                val manifest = getManifest()
                val newSession = createAuthorizationSession(
                    institution = requireNotNull(manifest.activeInstitution),
                    allowManualEntry = manifest.allowManualEntry
                )
                goNext(newSession.nextPane)
            } else {
                // For OAuth institutions, navigate to Session cancellation's next pane.
                goNext(result.nextPane)
            }
        }.onFailure {
            logger.error("failed cancelling session after cancelled web flow", it)
            setState { copy(authenticationStatus = Fail(it)) }
        }
    }

    private suspend fun completeAuthorizationSession() {
        kotlin.runCatching {
            setState { copy(authenticationStatus = Loading()) }
            val authSession = requireNotNull(getManifest().activeAuthSession)
            if (authSession.isOAuth) {
                logger.debug("Web AuthFlow completed! waiting for oauth results")
                val oAuthResults = pollAuthorizationSessionOAuthResults(authSession)
                logger.debug("OAuth results received! completing session")
                val updatedSession = completeAuthorizationSession(
                    authorizationSessionId = authSession.id,
                    publicToken = oAuthResults.publicToken
                )
                logger.debug("Session authorized!")
                goNext(updatedSession.nextPane)
            } else {
                goNext(NextPane.ACCOUNT_PICKER)
            }
        }.onFailure {
            logger.error("failed authorizing session", it)
            setState { copy(authenticationStatus = Fail(it)) }
        }
    }

    fun onEnterDetailsManuallyClick() {
        navigationManager.navigate(NavigationDirections.manualEntry)
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

internal data class PartnerAuthState(
    val payload: Async<Payload> = Uninitialized,
    val viewEffect: PartnerAuthViewEffect? = null,
    val authenticationStatus: Async<String> = Uninitialized
) : MavericksState {
    data class Payload(
        val institution: FinancialConnectionsInstitution,
        val flow: Flow?,
        val showPartnerDisclosure: Boolean,
        val showPrepane: Boolean
    )

    val canNavigateBack: Boolean
        get() =
            // Authentication running -> don't allow back navigation
            authenticationStatus !is Loading &&
                authenticationStatus !is Success &&
                // Failures posting institution -> don't allow back navigation
                payload !is Fail

    sealed interface PartnerAuthViewEffect {
        data class OpenPartnerAuth(val url: String) : PartnerAuthViewEffect
    }
}
