package com.stripe.android.financialconnections.features.networkinglinkloginwarmup

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.Click
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.domain.DisableNetworking
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.features.common.getBusinessName
import com.stripe.android.financialconnections.features.common.getRedactedEmail
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class NetworkingLinkLoginWarmupViewModel @Inject constructor(
    initialState: NetworkingLinkLoginWarmupState,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val getManifest: GetManifest,
    private val disableNetworking: DisableNetworking,
    private val navigationManager: NavigationManager,
    private val logger: Logger
) : MavericksViewModel<NetworkingLinkLoginWarmupState>(initialState) {

    init {
        logErrors()
        suspend {
            val manifest = getManifest()
            eventTracker.track(PaneLoaded(PANE))
            NetworkingLinkLoginWarmupState.Payload(
                merchantName = manifest.getBusinessName(),
                email = requireNotNull(manifest.getRedactedEmail())
            )
        }.execute { copy(payload = it) }
    }

    private fun logErrors() {
        onAsync(
            NetworkingLinkLoginWarmupState::payload,
            onFail = { error ->
                eventTracker.logError(
                    extraMessage = "Error fetching payload",
                    error = error,
                    logger = logger,
                    pane = PANE
                )
            },
        )
        onAsync(
            NetworkingLinkLoginWarmupState::disableNetworkingAsync,
            onFail = { error ->
                eventTracker.logError(
                    extraMessage = "Error disabling networking",
                    error = error,
                    logger = logger,
                    pane = PANE
                )
            },
        )
    }

    fun onContinueClick() = viewModelScope.launch {
        eventTracker.track(Click("click.continue", PANE))
        navigationManager.tryNavigateTo(Destination.NetworkingLinkVerification(referrer = PANE))
    }

    fun onClickableTextClick(text: String) = when (text) {
        CLICKABLE_TEXT_SKIP_LOGIN -> onSkipClicked()
        else -> logger.error("Unknown clicked text $text")
    }

    private fun onSkipClicked() {
        suspend {
            eventTracker.track(Click("click.skip_sign_in", PANE))
            disableNetworking().also {
                navigationManager.tryNavigateTo(
                    // skipping disables networking, which means
                    // we don't want the user to navigate back to
                    // the warm-up pane.
                    it.nextPane.destination(referrer = PANE),
                    popUpToCurrent = true,
                    inclusive = true
                )
            }
        }.execute { copy(disableNetworkingAsync = it) }
    }

    companion object :
        MavericksViewModelFactory<NetworkingLinkLoginWarmupViewModel, NetworkingLinkLoginWarmupState> {

        internal val PANE = Pane.NETWORKING_LINK_LOGIN_WARMUP

        private const val CLICKABLE_TEXT_SKIP_LOGIN = "skip_login"

        override fun create(
            viewModelContext: ViewModelContext,
            state: NetworkingLinkLoginWarmupState
        ): NetworkingLinkLoginWarmupViewModel {
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .networkingLinkLoginWarmupSubcomponent
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

internal data class NetworkingLinkLoginWarmupState(
    val payload: Async<Payload> = Uninitialized,
    val disableNetworkingAsync: Async<FinancialConnectionsSessionManifest> = Uninitialized,
) : MavericksState {

    data class Payload(
        val merchantName: String?,
        val email: String
    )
}
