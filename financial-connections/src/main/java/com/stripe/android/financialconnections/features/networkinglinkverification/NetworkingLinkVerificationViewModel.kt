package com.stripe.android.financialconnections.features.networkinglinkverification

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
import com.stripe.android.financialconnections.domain.GetConsumerSession
import com.stripe.android.financialconnections.domain.StartVerification
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.uicore.elements.OTPController
import javax.inject.Inject

internal class NetworkingLinkVerificationViewModel @Inject constructor(
    initialState: NetworkingLinkVerificationState,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val getConsumerSession: GetConsumerSession,
    private val startVerification: StartVerification,
    private val logger: Logger
) : MavericksViewModel<NetworkingLinkVerificationState>(initialState) {

    init {
        logErrors()
        suspend {
            val consumerSession = requireNotNull(getConsumerSession())
            val startVerification = startVerification(consumerSession.emailAddress)
            logger.debug(startVerification.verificationSessions.first().toString())
            eventTracker.track(PaneLoaded(Pane.NETWORKING_LINK_VERIFICATION))
            NetworkingLinkVerificationState.Payload(
                email = consumerSession.emailAddress,
                phoneNumber = consumerSession.redactedPhoneNumber,
                otpController = OTPController()
            )
        }.execute { copy(payload = it) }
    }

    private fun logErrors() {
        onAsync(
            NetworkingLinkVerificationState::payload,
            onFail = { error ->
                logger.error("Error fetching payload", error)
                eventTracker.track(Error(Pane.NETWORKING_LINK_VERIFICATION, error))
            },
        )
    }

    companion object :
        MavericksViewModelFactory<NetworkingLinkVerificationViewModel, NetworkingLinkVerificationState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: NetworkingLinkVerificationState
        ): NetworkingLinkVerificationViewModel {
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .networkingLinkVerificationSubcomponent
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

internal data class NetworkingLinkVerificationState(
    val payload: Async<Payload> = Uninitialized,
) : MavericksState {

    data class Payload(
        val email: String,
        val phoneNumber: String,
        val otpController: OTPController
    )
}
