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
import com.stripe.android.financialconnections.domain.CompleteAuthorizationSession
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.RequestNextStep
import com.stripe.android.financialconnections.domain.PollAuthorizationSessionOAuthResults
import com.stripe.android.financialconnections.exception.WebAuthFlowCancelledException
import com.stripe.android.financialconnections.exception.WebAuthFlowFailedException
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.Partner
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.FinancialConnectionsAuthorizationSession.Flow
import com.stripe.android.financialconnections.model.MixedOAuthParams
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
internal class PartnerAuthViewModel @Inject constructor(
    val completeAuthorizationSession: CompleteAuthorizationSession,
    val configuration: FinancialConnectionsSheet.Configuration,
    val getManifest: GetManifest,
    val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    val repository: FinancialConnectionsRepository,
    val pollAuthorizationSessionOAuthResults: PollAuthorizationSessionOAuthResults,
    val logger: Logger,
    initialState: PartnerAuthState
) : MavericksViewModel<PartnerAuthState>(initialState) {

    init {
        viewModelScope.launch {
            val manifest = getManifest()
            setState {
                copy(
                    institutionName = manifest.activeInstitution!!.name,
                    partner = manifest.activeAuthSession!!.flow!!.toPartner()
                )
            }
        }
    }

    private fun Flow.toPartner(): Partner = when (this) {
        Flow.FINICITY_CONNECT_V2_FIX,
        Flow.FINICITY_CONNECT_V2_LITE,
        Flow.FINICITY_CONNECT_V2_OAUTH,
        Flow.FINICITY_CONNECT_V2_OAUTH_REDIRECT,
        Flow.FINICITY_CONNECT_V2_OAUTH_WEBVIEW -> Partner.FINICITY
        Flow.MX_CONNECT,
        Flow.MX_OAUTH,
        Flow.MX_OAUTH_REDIRECT,
        Flow.MX_OAUTH_WEBVIEW -> Partner.MX
        Flow.TESTMODE,
        Flow.TESTMODE_OAUTH,
        Flow.TESTMODE_OAUTH_WEBVIEW -> Partner.TESTMODE
        Flow.TRUELAYER_OAUTH,
        Flow.TRUELAYER_OAUTH_HANDOFF,
        Flow.TRUELAYER_OAUTH_WEBVIEW -> Partner.TRUELAYER
        Flow.WELLS_FARGO_WEBVIEW,
        Flow.WELLS_FARGO -> Partner.WELLS_FARGO
    }

    fun onLaunchAuthClick() {
        viewModelScope.launch {
            val authSession = getManifest().activeAuthSession!!
            setState { copy(url = authSession.url) }
        }
    }

    fun onWebAuthFlowFinished(
        webStatus: Async<String>
    ) {
        viewModelScope.launch {
            val authSession = getManifest().activeAuthSession!!
            when (webStatus) {
                is Uninitialized -> {}
                is Loading -> setState { copy(authenticationStatus = Loading()) }
                is Success -> {
                    logger.debug("Web AuthFlow completed! waiting for oauth results")
                    val oAuthResults = pollAuthorizationSessionOAuthResults(authSession)
                    logger.debug("OAuth results received! completing session")
                    completeAuthorizationSession(
                        oAuthParams = oAuthResults,
                        authSession = authSession
                    )
                }
                is Fail -> {
                    setState { copy(authenticationStatus = webStatus) }
                    when (val error = webStatus.error) {
                        is WebAuthFlowCancelledException -> logger.debug("Web flow was cancelled")
                        is WebAuthFlowFailedException -> logger.debug("Web flow failed! ${error.url}")
                        else -> logger.error("error finishing web flow", error)
                    }
                }
            }
        }
    }

    private suspend fun completeAuthorizationSession(
        oAuthParams: MixedOAuthParams,
        authSession: FinancialConnectionsAuthorizationSession
    ) {
        kotlin.runCatching {
            completeAuthorizationSession(
                authorizationSessionId = authSession.id,
                publicToken = oAuthParams.memberGuid
            )
            logger.debug("Session authorized!")
            nativeAuthFlowCoordinator().emit(RequestNextStep(currentStep = NavigationDirections.partnerAuth))
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
    val institutionName: String = "",
    val partner: Partner = Partner.FINICITY,
    val url: String? = null,
    val authenticationStatus: Async<String> = Uninitialized
) : MavericksState {

    enum class Partner {
        FINICITY,
        MX,
        TESTMODE,
        TRUELAYER,
        WELLS_FARGO
    }
}
