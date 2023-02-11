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
import com.stripe.android.financialconnections.domain.ConfirmVerification
import com.stripe.android.financialconnections.domain.GetConsumerSession
import com.stripe.android.financialconnections.domain.StartVerification
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.model.ConsumerSession
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.uicore.elements.OTPElement
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class NetworkingLinkVerificationViewModel @Inject constructor(
    initialState: NetworkingLinkVerificationState,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val getConsumerSession: GetConsumerSession,
    private val startVerification: StartVerification,
    private val confirmVerification: ConfirmVerification,
    private val logger: Logger
) : MavericksViewModel<NetworkingLinkVerificationState>(initialState) {

    init {
        logErrors()
        suspend {
            val consumerSession = requireNotNull(getConsumerSession())
            val startVerification = startVerification(consumerSession.clientSecret)
            logger.debug(startVerification.verificationSessions.first().toString())
            eventTracker.track(PaneLoaded(Pane.NETWORKING_LINK_VERIFICATION))
            NetworkingLinkVerificationState.Payload(
                email = consumerSession.emailAddress,
                phoneNumber = consumerSession.redactedPhoneNumber,
                otpElement = OTPElement(
                    IdentifierSpec.Generic("otp"),
                    OTPController()
                )
            )
        }.execute { copy(payload = it) }
    }

    private fun logErrors() {
        onAsync(
            NetworkingLinkVerificationState::payload,
            onSuccess = {
                viewModelScope.launch {
                    it.otpElement.getFormFieldValueFlow()
                        .mapNotNull { formFieldsList ->
                            // formFieldsList contains only one element, for the OTP. Take the second value of
                            // the pair, which is the FormFieldEntry containing the value entered by the user.
                            formFieldsList.firstOrNull()?.second?.takeIf { it.isComplete }?.value
                        }.collectLatest { onOTPEntered(it) }
                }
            },
            onFail = { error ->
                logger.error("Error fetching payload", error)
                eventTracker.track(Error(Pane.NETWORKING_LINK_VERIFICATION, error))
            },
        )
    }

    private fun onOTPEntered(otp: String) {
        suspend {
            val consumerSession = requireNotNull(getConsumerSession())
            confirmVerification(
                consumerSessionClientSecret = consumerSession.clientSecret,
                verificationCode = otp
            )
        }.execute { copy(confirmVerification = it) }
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
    val confirmVerification: Async<ConsumerSession> = Uninitialized
) : MavericksState {

    data class Payload(
        val email: String,
        val phoneNumber: String,
        val otpElement: OTPElement
    )
}
