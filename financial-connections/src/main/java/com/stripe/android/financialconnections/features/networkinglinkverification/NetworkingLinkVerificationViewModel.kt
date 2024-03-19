package com.stripe.android.financialconnections.features.networkinglinkverification

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationError
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationError.Error.ConsumerNotFoundError
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationError.Error.LookupConsumerSession
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationError.Error.MarkLinkVerifiedError
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationError.Error.StartVerificationSessionError
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationSuccess
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.domain.ConfirmVerification
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.HandleError
import com.stripe.android.financialconnections.domain.LookupConsumerAndStartVerification
import com.stripe.android.financialconnections.domain.MarkLinkVerified
import com.stripe.android.financialconnections.features.networkinglinkverification.NetworkingLinkVerificationState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.Destination.InstitutionPicker
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.VerificationType
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.uicore.elements.OTPElement
import getRedactedPhoneNumber
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class NetworkingLinkVerificationViewModel @Inject constructor(
    initialState: NetworkingLinkVerificationState,
    private val getManifest: GetManifest,
    private val confirmVerification: ConfirmVerification,
    private val markLinkVerified: MarkLinkVerified,
    private val navigationManager: NavigationManager,
    private val analyticsTracker: FinancialConnectionsAnalyticsTracker,
    private val lookupConsumerAndStartVerification: LookupConsumerAndStartVerification,
    private val logger: Logger,
    private val handleError: HandleError,
) : MavericksViewModel<NetworkingLinkVerificationState>(initialState) {

    init {
        observeAsyncs()
        viewModelScope.launch {
            setState { copy(payload = Loading()) }
            runCatching { getManifest().also { requireNotNull(it.accountholderCustomerEmailAddress) } }
                .onSuccess { manifest ->
                    lookupConsumerAndStartVerification(
                        email = requireNotNull(manifest.accountholderCustomerEmailAddress),
                        businessName = manifest.businessName,
                        verificationType = VerificationType.SMS,
                        onConsumerNotFound = {
                            analyticsTracker.track(VerificationError(PANE, ConsumerNotFoundError))
                            navigationManager.tryNavigateTo(InstitutionPicker(referrer = PANE))
                        },
                        onLookupError = { error ->
                            analyticsTracker.track(VerificationError(PANE, LookupConsumerSession))
                            setState { copy(payload = Fail(error)) }
                        },
                        onStartVerification = { /* no-op */ },
                        onVerificationStarted = { consumerSession ->
                            val payload = buildPayload(consumerSession, manifest)
                            setState { copy(payload = Success(payload)) }
                        },
                        onStartVerificationError = { error ->
                            analyticsTracker.track(
                                VerificationError(PANE, StartVerificationSessionError)
                            )
                            setState { copy(payload = Fail(error)) }
                        }
                    )
                }
                .onFailure { setState { copy(payload = Fail(it)) } }
        }
    }

    private fun buildPayload(
        consumerSession: ConsumerSession,
        manifest: FinancialConnectionsSessionManifest
    ) = Payload(
        email = consumerSession.emailAddress,
        phoneNumber = consumerSession.getRedactedPhoneNumber(),
        initialInstitution = manifest.initialInstitution,
        consumerSessionClientSecret = consumerSession.clientSecret,
        otpElement = OTPElement(
            IdentifierSpec.Generic("otp"),
            OTPController()
        )
    )

    private fun observeAsyncs() {
        onAsync(
            NetworkingLinkVerificationState::payload,
            onSuccess = {
                viewModelScope.launch {
                    it.otpElement.otpCompleteFlow.collectLatest(::onOTPEntered)
                }
            },
            onFail = { error ->
                handleError(
                    extraMessage = "Error starting verification",
                    error = error,
                    pane = PANE,
                    displayErrorScreen = true,
                )
            },
        )
    }

    private fun onOTPEntered(otp: String) = suspend {
        val payload = requireNotNull(awaitState().payload())
        confirmVerification.sms(
            consumerSessionClientSecret = payload.consumerSessionClientSecret,
            verificationCode = otp
        )

        runCatching { markLinkVerified() }
            .fold(
                // TODO(carlosmuvi): once `/link_verified` is updated to return correct next_pane we should consume that
                onSuccess = {
                    analyticsTracker.track(VerificationSuccess(PANE))
                    navigationManager.tryNavigateTo(Destination.LinkAccountPicker(referrer = PANE))
                },
                onFailure = {
                    analyticsTracker.logError(
                        extraMessage = "Error confirming verification or marking link as verified",
                        error = it,
                        logger = logger,
                        pane = PANE
                    )
                    val nextPaneOnFailure = payload.initialInstitution
                        ?.let { Pane.PARTNER_AUTH }
                        ?: Pane.INSTITUTION_PICKER
                    analyticsTracker.track(VerificationError(PANE, MarkLinkVerifiedError))
                    navigationManager.tryNavigateTo(nextPaneOnFailure.destination(referrer = PANE))
                }
            )
    }.execute { copy(confirmVerification = it) }

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

        internal val PANE = Pane.NETWORKING_LINK_VERIFICATION
    }
}

internal data class NetworkingLinkVerificationState(
    val payload: Async<Payload> = Uninitialized,
    val confirmVerification: Async<Unit> = Uninitialized
) : MavericksState {

    data class Payload(
        val email: String,
        val phoneNumber: String,
        val otpElement: OTPElement,
        val consumerSessionClientSecret: String,
        val initialInstitution: FinancialConnectionsInstitution?
    )
}
