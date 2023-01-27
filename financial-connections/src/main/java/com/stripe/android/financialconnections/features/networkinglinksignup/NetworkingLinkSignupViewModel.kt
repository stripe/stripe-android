package com.stripe.android.financialconnections.features.networkinglinksignup

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
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import javax.inject.Inject

internal class NetworkingLinkSignupViewModel @Inject constructor(
    initialState: NetworkingLinkSignupState,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val logger: Logger
) : MavericksViewModel<NetworkingLinkSignupState>(initialState) {

    init {
        logErrors()
        suspend {
            eventTracker.track(PaneLoaded(Pane.NETWORKING_LINK_SIGNUP_PANE))
            Unit
        }.execute { copy(payload = it) }
    }

    private fun logErrors() {
        onAsync(
            NetworkingLinkSignupState::payload,
            onFail = { error ->
                logger.error("Error", error)
                eventTracker.track(Error(Pane.NETWORKING_LINK_SIGNUP_PANE, error))
            },
        )
    }

    companion object :
        MavericksViewModelFactory<NetworkingLinkSignupViewModel, NetworkingLinkSignupState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: NetworkingLinkSignupState
        ): NetworkingLinkSignupViewModel {
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .networkingLinkSignupSubcomponent
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

internal data class NetworkingLinkSignupState(
    val payload: Async<Unit> = Uninitialized
) : MavericksState
