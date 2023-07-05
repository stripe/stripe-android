package com.stripe.android.financialconnections.features.reset

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.Error
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.PaneLoaded
import com.stripe.android.financialconnections.domain.LinkMoreAccounts
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.ClearPartnerWebAuth
import com.stripe.android.financialconnections.domain.toNavigationCommand
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.NavigationState.NavigateToRoute
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import javax.inject.Inject

internal class ResetViewModel @Inject constructor(
    initialState: ResetState,
    private val linkMoreAccounts: LinkMoreAccounts,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val navigationManager: NavigationManager,
    private val logger: Logger
) : MavericksViewModel<ResetState>(initialState) {

    init {
        logErrors()
        suspend {
            val updatedManifest = linkMoreAccounts()
            nativeAuthFlowCoordinator().emit(ClearPartnerWebAuth)
            eventTracker.track(PaneLoaded(Pane.RESET))
            navigationManager.navigate(NavigateToRoute(updatedManifest.nextPane.toNavigationCommand()))
        }.execute { copy(payload = it) }
    }

    private fun logErrors() {
        onAsync(
            ResetState::payload,
            onFail = { error ->
                logger.error("Error linking more accounts", error)
                eventTracker.track(Error(Pane.RESET, error))
            },
        )
    }

    companion object : MavericksViewModelFactory<ResetViewModel, ResetState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: ResetState
        ): ResetViewModel {
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .resetSubcomponent
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

internal data class ResetState(
    val payload: Async<Unit> = Uninitialized
) : MavericksState
