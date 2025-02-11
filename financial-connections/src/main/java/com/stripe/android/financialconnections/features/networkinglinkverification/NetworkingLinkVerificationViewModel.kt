package com.stripe.android.financialconnections.features.networkinglinkverification

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationError
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationError.Error.MarkLinkVerifiedError
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationSuccess
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.AttachConsumerToLinkAccountSession
import com.stripe.android.financialconnections.domain.ConfirmVerification
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.GetOrFetchSync.RefetchCondition.Always
import com.stripe.android.financialconnections.domain.HandleError
import com.stripe.android.financialconnections.domain.IsLinkWithStripe
import com.stripe.android.financialconnections.domain.MarkLinkVerified
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.StartVerification
import com.stripe.android.financialconnections.features.networkinglinkverification.NetworkingLinkVerificationState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.repository.ConsumerSessionProvider
import com.stripe.android.financialconnections.utils.error
import com.stripe.android.model.ConsumerSession
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.uicore.elements.OTPElement
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import getRedactedPhoneNumber
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal class NetworkingLinkVerificationViewModel @AssistedInject constructor(
    @Assisted initialState: NetworkingLinkVerificationState,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val getOrFetchSync: GetOrFetchSync,
    private val confirmVerification: ConfirmVerification,
    private val markLinkVerified: MarkLinkVerified,
    private val navigationManager: NavigationManager,
    private val analyticsTracker: FinancialConnectionsAnalyticsTracker,
    private val startVerification: StartVerification,
    private val logger: Logger,
    private val isLinkWithStripe: IsLinkWithStripe,
    private val attachConsumerToLinkAccountSession: AttachConsumerToLinkAccountSession,
    private val consumerSessionProvider: ConsumerSessionProvider,
    private val handleError: HandleError,
) : FinancialConnectionsViewModel<NetworkingLinkVerificationState>(initialState, nativeAuthFlowCoordinator) {

    init {
        observeAsyncs()
        startVerification()
    }

    private fun startVerification() {
        suspend {
            val manifest = getOrFetchSync().manifest
            val cachedConsumerSession = requireNotNull(consumerSessionProvider.provideConsumerSession())
            val consumerSession = startVerification.sms(cachedConsumerSession.clientSecret)
            buildPayload(consumerSession, manifest.initialInstitution)
        }.execute { payload ->
            copy(payload = payload)
        }
    }

    override fun updateTopAppBar(state: NetworkingLinkVerificationState): TopAppBarStateUpdate {
        return TopAppBarStateUpdate(
            pane = PANE,
            allowBackNavigation = true,
            error = state.payload.error,
        )
    }

    private fun buildPayload(
        consumerSession: ConsumerSession,
        initialInstitution: FinancialConnectionsInstitution?,
    ) = Payload(
        email = consumerSession.emailAddress,
        phoneNumber = consumerSession.getRedactedPhoneNumber(),
        initialInstitution = initialInstitution,
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
                analyticsTracker.logError(
                    extraMessage = "Error starting verification",
                    error = error,
                    logger = logger,
                    pane = PANE
                )
            },
        )
    }

    private fun onOTPEntered(otp: String) = suspend {
        val payload = requireNotNull(stateFlow.value.payload())
        confirmVerification.sms(
            consumerSessionClientSecret = payload.consumerSessionClientSecret,
            verificationCode = otp
        )

        val isInstantDebits = isLinkWithStripe()

        runCatching {
            if (isInstantDebits) {
                attachConsumerToLinkAccountSession(payload.consumerSessionClientSecret)
                getOrFetchSync(refetchCondition = Always).manifest
            } else {
                markLinkVerified()
            }
        }.fold(
            // TODO(carlosmuvi): once `/link_verified` is updated to return correct next_pane we should consume that
            onSuccess = {
                analyticsTracker.track(VerificationSuccess(PANE))
                navigationManager.tryNavigateTo(Destination.LinkAccountPicker(referrer = PANE))
            },
            onFailure = {
                if (isInstantDebits) {
                    handleError(
                        extraMessage = "Error attaching consumer to LAS or synchronizing afterwards",
                        error = it,
                        pane = PANE,
                        displayErrorScreen = true,
                    )
                } else {
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
            }
        )
    }.execute { copy(confirmVerification = it) }

    @AssistedFactory
    interface Factory {
        fun create(initialState: NetworkingLinkVerificationState): NetworkingLinkVerificationViewModel
    }

    companion object {

        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    parentComponent.networkingLinkVerificationViewModelFactory.create(
                        NetworkingLinkVerificationState()
                    )
                }
            }

        internal val PANE = Pane.NETWORKING_LINK_VERIFICATION
    }
}

internal data class NetworkingLinkVerificationState(
    val payload: Async<Payload> = Uninitialized,
    val confirmVerification: Async<Unit> = Uninitialized
) {

    data class Payload(
        val email: String,
        val phoneNumber: String,
        val otpElement: OTPElement,
        val consumerSessionClientSecret: String,
        val initialInstitution: FinancialConnectionsInstitution?
    )
}
