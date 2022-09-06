package com.stripe.android.financialconnections.features.success

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.domain.CompleteFinancialConnectionsSession
import com.stripe.android.financialconnections.domain.GetAuthorizationSessionAccounts
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message
import com.stripe.android.financialconnections.features.common.AccessibleDataCalloutModel
import com.stripe.android.financialconnections.features.consent.FinancialConnectionsUrlResolver
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import javax.inject.Inject

internal class SuccessViewModel @Inject constructor(
    initialState: SuccessState,
    getAuthorizationSessionAccounts: GetAuthorizationSessionAccounts,
    getManifest: GetManifest,
    val logger: Logger,
    val navigationManager: NavigationManager,
    val completeFinancialConnectionsSession: CompleteFinancialConnectionsSession,
    val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator
) : MavericksViewModel<SuccessState>(initialState) {

    init {
        logErrors()
        suspend {
            val manifest = getManifest()
            SuccessState.Payload(
                accessibleData = AccessibleDataCalloutModel.fromManifest(manifest),
                accounts = getAuthorizationSessionAccounts(manifest.activeAuthSession!!.id),
                disconnectUrl = FinancialConnectionsUrlResolver.getDisconnectUrl(manifest),
                showLinkAnotherAccount = manifest.singleAccount.not() &&
                    manifest.disableLinkMoreAccounts.not() &&
                    manifest.isNetworkingUserFlow?.not() == true
            )
        }.execute {
            copy(payload = it)
        }
    }

    private fun logErrors() {
        onAsync(SuccessState::payload, onFail = {
            logger.error("Error retrieving payload", it)
        })
        onAsync(SuccessState::completeSession, onFail = {
            logger.error("Error completing session", it)
        })
    }

    fun onDoneClick() {
        suspend {
            completeFinancialConnectionsSession().also {
                nativeAuthFlowCoordinator().emit(Message.Finish)
            }
        }.execute { copy(completeSession = it) }
    }

    fun onLinkAnotherAccountClick() {
        navigationManager.navigate(NavigationDirections.reset)
    }

    companion object : MavericksViewModelFactory<SuccessViewModel, SuccessState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: SuccessState
        ): SuccessViewModel {
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .successSubcomponent
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

internal data class SuccessState(
    val payload: Async<Payload> = Uninitialized,
    val completeSession: Async<FinancialConnectionsSession> = Uninitialized
) : MavericksState {
    data class Payload(
        val accessibleData: AccessibleDataCalloutModel,
        val showLinkAnotherAccount: Boolean,
        val accounts: PartnerAccountsList,
        val disconnectUrl: String
    )
}
