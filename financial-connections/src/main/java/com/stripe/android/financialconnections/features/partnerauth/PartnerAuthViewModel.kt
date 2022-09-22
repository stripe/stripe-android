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
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.domain.CancelAuthorizationSession
import com.stripe.android.financialconnections.domain.CompleteAuthorizationSession
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.domain.PollAuthorizationSessionOAuthResults
import com.stripe.android.financialconnections.domain.PostAuthorizationSession
import com.stripe.android.financialconnections.exception.WebAuthFlowCancelledException
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.FinancialConnectionsAuthorizationSession.Flow
import com.stripe.android.financialconnections.model.MixedOAuthParams
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
internal class PartnerAuthViewModel @Inject constructor(
    val completeAuthorizationSession: CompleteAuthorizationSession,
    val createAuthorizationSession: PostAuthorizationSession,
    val cancelAuthorizationSession: CancelAuthorizationSession,
    val configuration: FinancialConnectionsSheet.Configuration,
    val getManifest: GetManifest,
    val goNext: GoNext,
    val navigationManager: NavigationManager,
    val pollAuthorizationSessionOAuthResults: PollAuthorizationSessionOAuthResults,
    val logger: Logger,
    initialState: PartnerAuthState
) : MavericksViewModel<PartnerAuthState>(initialState) {

    init {
        logErrors()
        suspend {
            val manifest = getManifest()
            val authSession = createAuthorizationSession(manifest.activeInstitution!!)
            Payload(
                flow = authSession.flow,
                showPartnerDisclosure = authSession.showPartnerDisclosure ?: false,
                institutionName = manifest.activeInstitution.name,
            )
        }.execute {
            copy(payload = it)
        }
    }

    private fun logErrors() {
        onAsync(PartnerAuthState::payload, onFail = {
            logger.error("Error fetching payload / posting AuthSession", it)
        })
    }

    fun onLaunchAuthClick() {
        viewModelScope.launch {
            val authSession = getManifest().activeAuthSession!!
            setState { copy(url = authSession.url) }
        }
    }

    fun onSelectAnotherBank() {
        navigationManager.navigate(NavigationDirections.institutionPicker)
    }

    fun onWebAuthFlowFinished(
        webStatus: Async<String>
    ) {
        logger.debug("Web AuthFlow status received $webStatus")
        viewModelScope.launch {
            when (webStatus) {
                is Uninitialized -> {}
                is Loading -> setState { copy(authenticationStatus = Loading()) }
                is Success -> {
                    val authSession = getManifest().activeAuthSession!!
                    logger.debug("Web AuthFlow completed! waiting for oauth results")
                    val oAuthResults = pollAuthorizationSessionOAuthResults(authSession)
                    logger.debug("OAuth results received! completing session")
                    completeAuthorizationSession(
                        oAuthParams = oAuthResults,
                        authSession = authSession
                    )
                }
                is Fail -> {
                    val authSession = getManifest().activeAuthSession!!
                    when (val error = webStatus.error) {
                        is WebAuthFlowCancelledException -> onAuthCancelled(authSession)
                        else -> onAuthFailed(error, authSession)
                    }
                }
            }
        }
    }

    private suspend fun onAuthFailed(
        error: Throwable,
        authSession: FinancialConnectionsAuthorizationSession
    ) {
        kotlin.runCatching {
            logger.error("Auth failed, cancelling AuthSession", error)
            cancelAuthorizationSession(authSession.id)
            setState { copy(authenticationStatus = Fail(error)) }
        }.onFailure {
            logger.error("failed cancelling session after failed web flow", it)
        }
    }

    private suspend fun onAuthCancelled(
        authSession: FinancialConnectionsAuthorizationSession
    ) {
        setState { copy(authenticationStatus = Loading()) }
        kotlin.runCatching {
            logger.debug("Auth cancelled, cancelling AuthSession")
            cancelAuthorizationSession(authSession.id)
            logger.debug("Session cancelled, creating a new one for same institution")
            val newSession = createAuthorizationSession(getManifest().activeInstitution!!)
            goNext(newSession.nextPane)
        }.onFailure {
            logger.error("failed cancelling session after cancelled web flow", it)
            setState { copy(authenticationStatus = Fail(it)) }
        }
    }

    private suspend fun completeAuthorizationSession(
        oAuthParams: MixedOAuthParams,
        authSession: FinancialConnectionsAuthorizationSession
    ) {
        kotlin.runCatching {
            val updatedSession = completeAuthorizationSession(
                authorizationSessionId = authSession.id,
                publicToken = oAuthParams.memberGuid
            )
            logger.debug("Session authorized!")
            goNext(updatedSession.nextPane)
        }.onFailure {
            logger.error("failed authorizing session", it)
            setState { copy(authenticationStatus = Fail(it)) }
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
    val url: String? = null,
    val authenticationStatus: Async<String> = Uninitialized
) : MavericksState {
    data class Payload(
        val institutionName: String,
        val flow: Flow?,
        val showPartnerDisclosure: Boolean
    )
}
