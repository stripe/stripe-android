package com.stripe.android.financialconnections.features.reset

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.LinkMoreAccounts
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator.Message.ClearPartnerWebAuth
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.utils.error
import com.stripe.android.uicore.navigation.NavigationManager
import com.stripe.android.uicore.navigation.PopUpToBehavior
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class ResetViewModel @AssistedInject constructor(
    @Assisted initialState: ResetState,
    private val linkMoreAccounts: LinkMoreAccounts,
    private val nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val navigationManager: NavigationManager,
    private val logger: Logger
) : FinancialConnectionsViewModel<ResetState>(initialState, nativeAuthFlowCoordinator) {

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

    override fun updateTopAppBar(state: ResetState): TopAppBarStateUpdate = TopAppBarStateUpdate(
        pane = PANE,
        allowBackNavigation = false,
        error = state.payload.error,
    )

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

    @AssistedFactory
    interface Factory {
        fun create(initialState: ResetState): ResetViewModel
    }

    companion object {

        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    parentComponent.resetViewModelFactory.create(ResetState())
                }
            }

        internal val PANE = Pane.RESET
    }
}

internal data class ResetState(
    val payload: Async<Unit> = Uninitialized
)
