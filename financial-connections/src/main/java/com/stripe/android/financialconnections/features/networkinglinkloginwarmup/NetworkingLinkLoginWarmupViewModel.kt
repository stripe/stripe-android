package com.stripe.android.financialconnections.features.networkinglinkloginwarmup

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
import com.stripe.android.financialconnections.domain.DisableNetworking
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.features.consent.ConsentTextBuilder
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import javax.inject.Inject

internal class NetworkingLinkLoginWarmupViewModel @Inject constructor(
    initialState: NetworkingLinkLoginWarmupState,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val getManifest: GetManifest,
    private val disableNetworking: DisableNetworking,
    private val goNext: GoNext,
    private val logger: Logger
) : MavericksViewModel<NetworkingLinkLoginWarmupState>(initialState) {

    init {
        logErrors()
        suspend {
            val manifest = getManifest()
            eventTracker.track(PaneLoaded(Pane.NETWORKING_LINK_SIGNUP_PANE))
            val emailAddress = requireNotNull(manifest.accountholderCustomerEmailAddress)
            NetworkingLinkLoginWarmupState.Payload(
                merchantName = ConsentTextBuilder.getBusinessName(manifest),
                email = emailAddress
            )
        }.execute { copy(payload = it) }
    }

    private fun logErrors() {
        onAsync(
            NetworkingLinkLoginWarmupState::payload,
            onFail = { error ->
                logger.error("Error fetching payload", error)
                eventTracker.track(Error(Pane.NETWORKING_LINK_SIGNUP_PANE, error))
            },
        )
        onAsync(
            NetworkingLinkLoginWarmupState::disableNetworkingAsync,
            onFail = { error ->
                logger.error("Error disabling networking", error)
                eventTracker.track(Error(Pane.NETWORKING_LINK_SIGNUP_PANE, error))
            },
        )
    }

    fun onContinueClick() {
        goNext(Pane.NETWORKING_LINK_VERIFICATION)
    }

    fun onClickableTextClick(text: String) = when (text) {
        CLICKABLE_TEXT_SKIP_LOGIN -> onSkipClicked()
        else -> logger.error("Unknown clicked text $text")
    }

    private fun onSkipClicked() {
        suspend {
            disableNetworking().also { goNext(it.nextPane) }
        }.execute { copy(disableNetworkingAsync = it) }
    }

    companion object :
        MavericksViewModelFactory<NetworkingLinkLoginWarmupViewModel, NetworkingLinkLoginWarmupState> {

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

private const val CLICKABLE_TEXT_SKIP_LOGIN = "skip_login"

internal data class NetworkingLinkLoginWarmupState(
    val payload: Async<Payload> = Uninitialized,
    val disableNetworkingAsync: Async<FinancialConnectionsSessionManifest> = Uninitialized,
) : MavericksState {

    data class Payload(
        val merchantName: String?,
        val email: String
    )
}
