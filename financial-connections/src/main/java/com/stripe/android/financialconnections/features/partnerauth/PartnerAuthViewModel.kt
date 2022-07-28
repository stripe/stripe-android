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
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.UpdateAuthorizationSession
import com.stripe.android.financialconnections.domain.PollAuthorizationSessionOAuthResults
import com.stripe.android.financialconnections.exception.WebAuthFlowCancelledException
import com.stripe.android.financialconnections.exception.WebAuthFlowFailedException
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.MixedOAuthParams
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class PartnerAuthViewModel @Inject constructor(
    val completeAuthorizationSession: CompleteAuthorizationSession,
    val configuration: FinancialConnectionsSheet.Configuration,
    val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    val repository: FinancialConnectionsRepository,
    val pollAuthorizationSessionOAuthResults: PollAuthorizationSessionOAuthResults,
    val logger: Logger
) : MavericksViewModel<PartnerAuthState>(PartnerAuthState()) {

    fun onWebAuthFlowFinished(
        webStatus: Async<String>,
        authSession: FinancialConnectionsAuthorizationSession
    ) {
        viewModelScope.launch {
            when (webStatus) {
                is Uninitialized,
                is Loading -> setState { copy(title = "Web flow in progress...") }
                is Success -> {
                    setState { copy(title = "Web AuthFlow completed! waiting for oauth results") }
                    val oAuthResults = pollAuthorizationSessionOAuthResults(authSession)
                    setState { copy(title = "OAuth results received! completing session") }
                    completeAuthorizationSession(
                        oAuthParams = oAuthResults,
                        authSession = authSession
                    )
                }
                is Fail -> {
                    when (val error = webStatus.error) {
                        is WebAuthFlowCancelledException ->
                            setState { copy(title = "Web flow was cancelled") }
                        is WebAuthFlowFailedException -> setState { copy(title = "Web flow failed! ${error.url}") }
                        else -> setState {
                            logger.error("error finishing web flow", error)
                            copy(title = "Web flow failed! unknown")
                        }
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
            val session = completeAuthorizationSession(
                authorizationSessionId = authSession.id,
                publicToken = oAuthParams.memberGuid
            )
            setState { copy(title = "Session authorized! Start polling accounts.") }
            nativeAuthFlowCoordinator().emit(UpdateAuthorizationSession(session))
            // TODO@carlosmuvi start polling until accounts ready, then navigate to next pane.
//            nativeAuthFlowCoordinator().emit(
//                RequestNextStep(currentStep = NavigationDirections.partnerAuth)
//            )
        }.onFailure {
            logger.error("failed authorizing session", it)
            setState { copy(title = "Failed authorizing session!") }
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
    val title: String = ""
) : MavericksState
