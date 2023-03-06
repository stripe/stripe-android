package com.stripe.android.financialconnections.features.networkingsavetolinkverification

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
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetCachedConsumerSession
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.domain.MarkLinkVerified
import com.stripe.android.financialconnections.domain.SaveAccountToLink
import com.stripe.android.financialconnections.domain.StartVerification
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.uicore.elements.OTPElement
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class NetworkingSaveToLinkVerificationViewModel @Inject constructor(
    initialState: NetworkingSaveToLinkVerificationState,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val getCachedConsumerSession: GetCachedConsumerSession,
    private val startVerification: StartVerification,
    private val confirmVerification: ConfirmVerification,
    private val markLinkVerified: MarkLinkVerified,
    private val getCachedAccounts: GetCachedAccounts,
    private val saveAccountToLink: SaveAccountToLink,
    private val goNext: GoNext,
    private val logger: Logger
) : MavericksViewModel<NetworkingSaveToLinkVerificationState>(initialState) {

    init {
        logErrors()
        suspend {
            val consumerSession = requireNotNull(getCachedConsumerSession())
            startVerification.sms(consumerSession.clientSecret)
            eventTracker.track(PaneLoaded(Pane.NETWORKING_SAVE_TO_LINK_VERIFICATION))
            NetworkingSaveToLinkVerificationState.Payload(
                email = consumerSession.emailAddress,
                phoneNumber = consumerSession.redactedPhoneNumber,
                consumerSessionClientSecret = consumerSession.clientSecret,
                otpElement = OTPElement(
                    IdentifierSpec.Generic("otp"),
                    OTPController()
                )
            )
        }.execute { copy(payload = it) }
    }

    private fun logErrors() {
        onAsync(
            NetworkingSaveToLinkVerificationState::payload,
            onSuccess = {
                viewModelScope.launch {
                    it.otpElement.otpCompleteFlow.collectLatest { onOTPEntered(it) }
                }
            },
            onFail = { error ->
                logger.error("Error fetching payload", error)
                eventTracker.track(Error(Pane.NETWORKING_SAVE_TO_LINK_VERIFICATION, error))
            },
        )
        onAsync(
            NetworkingSaveToLinkVerificationState::confirmVerification,
            onFail = { error ->
                logger.error("Error confirming verification", error)
                eventTracker.track(Error(Pane.NETWORKING_SAVE_TO_LINK_VERIFICATION, error))
            },
        )
    }

    private fun onOTPEntered(otp: String) = suspend {
        val payload = requireNotNull(awaitState().payload())
        confirmVerification.sms(
            consumerSessionClientSecret = payload.consumerSessionClientSecret,
            verificationCode = otp
        )
        // save accounts to link
        val selectedAccounts = getCachedAccounts()
        saveAccountToLink.existing(
            consumerSessionClientSecret = payload.consumerSessionClientSecret,
            selectedAccounts = selectedAccounts.map { it.id },
        )
        // Mark link verified
        markLinkVerified()
        goNext(Pane.SUCCESS)
        Unit
    }.execute { copy(confirmVerification = it) }

    fun onSkipClick() {
        goNext(Pane.SUCCESS)
    }

    companion object :
        MavericksViewModelFactory<NetworkingSaveToLinkVerificationViewModel, NetworkingSaveToLinkVerificationState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: NetworkingSaveToLinkVerificationState
        ): NetworkingSaveToLinkVerificationViewModel {
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .networkingSaveToLinkVerificationSubcomponent
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

internal data class NetworkingSaveToLinkVerificationState(
    val payload: Async<Payload> = Uninitialized,
    val confirmVerification: Async<Unit> = Uninitialized
) : MavericksState {

    data class Payload(
        val email: String,
        val phoneNumber: String,
        val otpElement: OTPElement,
        val consumerSessionClientSecret: String
    )
}
