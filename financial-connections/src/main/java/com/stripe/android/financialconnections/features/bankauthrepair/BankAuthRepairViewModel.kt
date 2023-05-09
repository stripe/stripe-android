package com.stripe.android.financialconnections.features.bankauthrepair

import android.webkit.URLUtil
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.AuthSessionEvent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.PaneLoaded
import com.stripe.android.financialconnections.di.APPLICATION_ID
import com.stripe.android.financialconnections.domain.CreateRepairSession
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.domain.PollAuthorizationSessionOAuthResults
import com.stripe.android.financialconnections.domain.PostAuthSessionEvent
import com.stripe.android.financialconnections.domain.UpdateLocalManifest
import com.stripe.android.financialconnections.exception.WebAuthFlowCancelledException
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.ClickableText
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.Payload
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.ViewEffect.OpenBottomSheet
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.ViewEffect.OpenPartnerAuth
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationRepairSession
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.repository.PartnerToCoreAuthsRepository
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.utils.UriUtils
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

@Suppress("LongParameterList")
internal class BankAuthRepairViewModel @Inject constructor(
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    @Named(APPLICATION_ID) private val applicationId: String,
    private val uriUtils: UriUtils,
    private val createRepairSession: CreateRepairSession,
    private val updateLocalManifest: UpdateLocalManifest,
    private val postAuthSessionEvent: PostAuthSessionEvent,
    private val partnerToCoreAuthsRepository: PartnerToCoreAuthsRepository,
    private val getManifest: GetManifest,
    private val getCachedAccounts: GetCachedAccounts,
    private val goNext: GoNext,
    private val navigationManager: NavigationManager,
    private val pollAuthorizationSessionOAuthResults: PollAuthorizationSessionOAuthResults,
    private val logger: Logger,
    initialState: PartnerAuthState
) : MavericksViewModel<PartnerAuthState>(initialState) {
    init {
        logErrors()
        observePayload()
        suspend {
//            val launchedEvent = Launched(Date())
            val selectedAccount = getCachedAccounts().first()
            val coreAuthorization = partnerToCoreAuthsRepository.get()!!.getValue(selectedAccount.authorization)
            logger.debug(coreAuthorization)
            val repairSession = createRepairSession(coreAuthorization)
            updateLocalManifest {
                it.copy(activeInstitution = repairSession.institution)
            }
            Payload(
                authSession = repairSession.toAuthSession(),
                institution = requireNotNull(repairSession.institution),
                isStripeDirect = getManifest().isStripeDirect ?: false
            ).also {
                // just send loaded event on OAuth flows (prepane). Non-OAuth handled by shim.
//                val loadedEvent: Loaded? = Loaded(Date()).takeIf { authSession.isOAuth }
//                postAuthSessionEvent(
//                    authSession.id,
//                    listOfNotNull(launchedEvent, loadedEvent)
//                )
            }
        }.execute {
            copy(payload = it)
        }
    }

    private fun FinancialConnectionsAuthorizationRepairSession.toAuthSession(): FinancialConnectionsAuthorizationSession {
        return FinancialConnectionsAuthorizationSession(
            id = this.id,
            url = this.url,
            nextPane = Pane.SUCCESS,
            _isOAuth = true,
        )
    }


    private fun observePayload() {
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
                logger.error("Error fetching payload / posting AuthSession", it)
                eventTracker.track(FinancialConnectionsEvent.Error(Pane.PARTNER_AUTH, it))
            },
            onSuccess = { eventTracker.track(PaneLoaded(Pane.PARTNER_AUTH)) }
        )
    }

    fun onLaunchAuthClick() {
        viewModelScope.launch {
            awaitState().payload()?.authSession?.let {
                postAuthSessionEvent(it.id, AuthSessionEvent.OAuthLaunched(Date()))
            }
            launchAuthInBrowser()
        }
    }

    private suspend fun launchAuthInBrowser() {
        kotlin.runCatching { requireNotNull(getManifest().activeAuthSession) }
            .onSuccess {
                it.url
                    ?.replaceFirst("stripe-auth://native-redirect/$applicationId/", "")
                    ?.let { setState { copy(viewEffect = OpenPartnerAuth(it)) } }
            }
            .onFailure {
                eventTracker.track(FinancialConnectionsEvent.Error(Pane.PARTNER_AUTH, it))
                logger.error("failed retrieving active session from cache", it)
                setState { copy(authenticationStatus = Fail(it)) }
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
//        kotlin.runCatching {
//            logger.debug("Auth failed, cancelling AuthSession")
//            val authSession = getManifest().activeAuthSession
//            logger.error("Auth failed, cancelling AuthSession", error)
//            when {
//                authSession != null -> {
//                    postAuthSessionEvent(authSession.id, AuthSessionEvent.Failure(Date(), error))
//                    cancelAuthorizationSession(authSession.id)
//                }
//
//                else -> logger.debug("Could not find AuthSession to cancel.")
//            }
//            setState { copy(authenticationStatus = Fail(error)) }
//        }.onFailure {
//            logger.error("failed cancelling session after failed web flow", it)
//        }
    }

    private suspend fun onAuthCancelled() {
//        kotlin.runCatching {
//            logger.debug("Auth cancelled, cancelling AuthSession")
//            setState { copy(authenticationStatus = Loading()) }
//            val authSession = requireNotNull(getManifest().activeAuthSession)
//            val result = cancelAuthorizationSession(authSession.id)
//            if (authSession.isOAuth) {
//                // For OAuth institutions, create a new session and navigate to its nextPane (prepane).
//                logger.debug("Creating a new session for this OAuth institution")
//                // Send retry event as we're presenting the prepane again.
//                postAuthSessionEvent(authSession.id, AuthSessionEvent.Retry(Date()))
//                val manifest = getManifest()
//                val newSession = createAuthorizationSession(
//                    institution = requireNotNull(manifest.activeInstitution),
//                    allowManualEntry = manifest.allowManualEntry
//                )
//                goNext(newSession.nextPane)
//            } else {
//                // For OAuth institutions, navigate to Session cancellation's next pane.
//                postAuthSessionEvent(authSession.id, AuthSessionEvent.Cancel(Date()))
//                goNext(result.nextPane)
//            }
//        }.onFailure {
//            logger.error("failed cancelling session after cancelled web flow", it)
//            setState { copy(authenticationStatus = Fail(it)) }
//        }
    }

    private suspend fun completeAuthorizationSession() {
//        kotlin.runCatching {
//            setState { copy(authenticationStatus = Loading()) }
//            val authSession = requireNotNull(getManifest().activeAuthSession)
//            postAuthSessionEvent(authSession.id, AuthSessionEvent.Success(Date()))
//            if (authSession.isOAuth) {
//                logger.debug("Web AuthFlow completed! waiting for oauth results")
//                val oAuthResults = pollAuthorizationSessionOAuthResults(authSession)
//                logger.debug("OAuth results received! completing session")
//                val updatedSession = completeAuthorizationSession(
//                    authorizationSessionId = authSession.id,
//                    publicToken = oAuthResults.publicToken
//                )
//                logger.debug("Session authorized!")
//                goNext(updatedSession.nextPane)
//            } else {
//                goNext(Pane.ACCOUNT_PICKER)
//            }
//        }.onFailure {
//            logger.error("failed authorizing session", it)
//            setState { copy(authenticationStatus = Fail(it)) }
//        }
    }

    fun onEnterDetailsManuallyClick() {
        navigationManager.navigate(NavigationDirections.manualEntry)
    }

    fun onClickableTextClick(uri: String) {
        // if clicked uri contains an eventName query param, track click event.
        viewModelScope.launch {
            uriUtils.getQueryParameter(uri, "eventName")?.let { eventName ->
                eventTracker.track(
                    FinancialConnectionsEvent.Click(
                        eventName,
                        pane = Pane.PARTNER_AUTH
                    )
                )
            }
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
            val managedUri = ClickableText.values()
                .firstOrNull { uriUtils.compareSchemeAuthorityAndPath(it.value, uri) }
            when (managedUri) {
                ClickableText.DATA -> {
                    setState {
                        copy(
                            viewEffect = OpenBottomSheet(Date().time)
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

    companion object : MavericksViewModelFactory<BankAuthRepairViewModel, PartnerAuthState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: PartnerAuthState
        ): BankAuthRepairViewModel {
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .bankAuthRepairSubcomponent
                .initialState(state)
                .build()
                .viewModel
        }
    }
}
