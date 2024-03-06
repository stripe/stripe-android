package com.stripe.android.financialconnections.features.reset

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.domain.LinkMoreAccounts
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.ClearPartnerWebAuth
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.PopUpToBehavior
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.presentation.ScreenViewModel
import com.stripe.android.financialconnections.presentation.TopAppBarHost
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import javax.inject.Inject

internal class ResetViewModel @Inject constructor(
    initialState: ResetState,
    topAppBarHost: TopAppBarHost,
    private val linkMoreAccounts: LinkMoreAccounts,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val navigationManager: NavigationManager,
    private val logger: Logger
) : ScreenViewModel<ResetState>(initialState, topAppBarHost, Pane.RESET) {

    init {
        logErrors()
        suspend {
            val updatedManifest = linkMoreAccounts()
            nativeAuthFlowCoordinator().emit(ClearPartnerWebAuth)
            eventTracker.track(PaneLoaded(PANE))
            navigationManager.tryNavigateTo(
                route = updatedManifest.nextPane.destination(referrer = PANE),
                popUpTo = PopUpToBehavior.Current(inclusive = true),
            )
        }.execute { copy(payload = it) }
    }

    override fun allowsBackNavigation(state: ResetState): Boolean {
        return true
    }

    override fun hidesStripeLogo(state: ResetState, originalValue: Boolean): Boolean {
        return originalValue
    }

    private fun logErrors() {
        onAsync(
            ResetState::payload,
            onFail = { error ->
                eventTracker.logError(
                    extraMessage = "Error linking more accounts",
                    error = error,
                    logger = logger,
                    pane = PANE
                )
            },
        )
    }

    companion object : MavericksViewModelFactory<ResetViewModel, ResetState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: ResetState
        ): ResetViewModel {
            val parentViewModel = viewModelContext.activity<FinancialConnectionsSheetNativeActivity>().viewModel

            return parentViewModel
                .activityRetainedComponent
                .resetSubcomponent
                .initialState(state)
                .topAppBarHost(parentViewModel)
                .build()
                .viewModel
        }

        internal val PANE = Pane.RESET
    }
}

internal data class ResetState(
    val payload: Async<Unit> = Uninitialized
) : MavericksState
