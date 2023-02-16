package com.stripe.android.financialconnections.features.linkstepupverification

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
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.GoNext
import com.stripe.android.financialconnections.domain.LookupAccount
import com.stripe.android.financialconnections.domain.StartVerification
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.uicore.elements.OTPElement
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class LinkStepUpVerificationViewModel @Inject constructor(
    initialState: LinkStepUpVerificationState,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val getManifest: GetManifest,
    private val lookupAccount: LookupAccount,
    private val startVerification: StartVerification,
    private val confirmVerification: ConfirmVerification,
    private val goNext: GoNext,
    private val logger: Logger
) : MavericksViewModel<LinkStepUpVerificationState>(initialState) {

    init {
        logErrors()
        suspend {
            val email = requireNotNull(getManifest().accountholderCustomerEmailAddress)
            val consumerSession = requireNotNull(lookupAccount(email).consumerSession)
            startVerification(consumerSession.clientSecret)
            eventTracker.track(PaneLoaded(Pane.LINK_STEP_UP_VERIFICATION))
            LinkStepUpVerificationState.Payload(
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
            LinkStepUpVerificationState::payload,
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

    private fun onOTPEntered(otp: String) = suspend {
        val payload = requireNotNull(awaitState().payload())
        confirmVerification(
            consumerSessionClientSecret = payload.consumerSessionClientSecret,
            verificationCode = otp
        )
        goNext(Pane.SUCCESS)
        //TODO select accounts!
        Unit
    }.execute { copy(confirmVerification = it) }

    companion object :
        MavericksViewModelFactory<LinkStepUpVerificationViewModel, LinkStepUpVerificationState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: LinkStepUpVerificationState
        ): LinkStepUpVerificationViewModel {
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .linkStepUpVerificationSubcomponent
                .initialState(state)
                .build()
                .viewModel
        }
    }
}

internal data class LinkStepUpVerificationState(
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
