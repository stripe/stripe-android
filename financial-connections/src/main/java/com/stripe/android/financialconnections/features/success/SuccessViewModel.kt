package com.stripe.android.financialconnections.features.success

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.ClickDisconnectLink
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.ClickDone
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.ClickLearnMoreDataAccess
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.ClickLinkAnotherAccount
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Complete
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.PaneLoaded
import com.stripe.android.financialconnections.domain.CompleteFinancialConnectionsSession
import com.stripe.android.financialconnections.domain.GetAuthorizationSessionAccounts
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message
import com.stripe.android.financialconnections.features.common.AccessibleDataCalloutModel
import com.stripe.android.financialconnections.features.consent.FinancialConnectionsUrlResolver
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetActivityResult.Completed
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.ClientPane
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
internal class SuccessViewModel @Inject constructor(
    initialState: SuccessState,
    getAuthorizationSessionAccounts: GetAuthorizationSessionAccounts,
    getManifest: GetManifest,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val logger: Logger,
    private val navigationManager: NavigationManager,
    private val completeFinancialConnectionsSession: CompleteFinancialConnectionsSession,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator
) : MavericksViewModel<SuccessState>(initialState) {

    init {
        logErrors()

        suspend {
            val manifest = getManifest()
            SuccessState.Payload(
                accessibleData = AccessibleDataCalloutModel.fromManifest(manifest),
                accounts = getAuthorizationSessionAccounts(manifest.activeAuthSession!!.id),
                institution = manifest.activeInstitution!!,
                businessName = manifest.businessName,
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
        onAsync(
            SuccessState::payload,
            onFail = { logger.error("Error retrieving payload", it) },
            onSuccess = { eventTracker.track(PaneLoaded(ClientPane.SUCCESS)) }
        )
        onAsync(SuccessState::completeSession, onSuccess = {
            eventTracker.track(
                Complete(
                    connectedAccounts = it.accounts.data.count(),
                    exception = null
                )
            )
        }, onFail = {
                eventTracker.track(
                    Complete(
                        connectedAccounts = null,
                        exception = it
                    )
                )
                logger.error("Error completing session", it)
            })
    }

    fun onDoneClick() {
        viewModelScope.launch {
            eventTracker.track(ClickDone(ClientPane.SUCCESS))
        }
        suspend {
            completeFinancialConnectionsSession().also {
                val result = Completed(
                    financialConnectionsSession = it,
                    token = it.parsedToken
                )
                nativeAuthFlowCoordinator().emit(Message.Finish(result))
            }
        }.execute { copy(completeSession = it) }
    }

    fun onLinkAnotherAccountClick() {
        viewModelScope.launch {
            eventTracker.track(ClickLinkAnotherAccount(ClientPane.SUCCESS))
        }
        navigationManager.navigate(NavigationDirections.reset)
    }

    fun onLearnMoreAboutDataAccessClick() {
        viewModelScope.launch {
            eventTracker.track(ClickLearnMoreDataAccess(ClientPane.SUCCESS))
        }
    }

    fun onDisconnectLinkClick() {
        viewModelScope.launch {
            eventTracker.track(ClickDisconnectLink(ClientPane.SUCCESS))
        }
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
        val institution: FinancialConnectionsInstitution,
        val accounts: PartnerAccountsList,
        val disconnectUrl: String,
        val businessName: String?
    )
}
