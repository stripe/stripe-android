package com.stripe.android.financialconnections.features.networkinglinkverification

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationError
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationError.Error.ConsumerNotFoundError
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationError.Error.LookupConsumerSession
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationError.Error.MarkLinkVerifiedError
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationError.Error.StartVerificationSessionError
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
import com.stripe.android.financialconnections.domain.LookupConsumerAndStartVerification
import com.stripe.android.financialconnections.domain.MarkLinkVerified
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.features.networkinglinkverification.NetworkingLinkVerificationState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.Destination.InstitutionPicker
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.destination
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Fail
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Success
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.repository.ConsumerSessionProvider
import com.stripe.android.financialconnections.utils.error
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.VerificationType
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
    private val lookupConsumerAndStartVerification: LookupConsumerAndStartVerification,
    private val logger: Logger,
    private val isLinkWithStripe: IsLinkWithStripe,
    private val attachConsumerToLinkAccountSession: AttachConsumerToLinkAccountSession,
    private val consumerSessionProvider: ConsumerSessionProvider,
    private val handleError: HandleError,
) : FinancialConnectionsViewModel<NetworkingLinkVerificationState>(initialState, nativeAuthFlowCoordinator) {

    init {
        observeAsyncs()
        viewModelScope.launch {
            setState { copy(payload = Loading()) }

            runCatching {
                buildInitData()
            }.onSuccess {
                startVerification(it)
            }.onFailure {
                setState { copy(payload = Fail(it)) }
            }
        }
    }

    private suspend fun buildInitData(): InitData {
        val manifest = getOrFetchSync().manifest
        val isInstantDebits = isLinkWithStripe()

        val email = if (isInstantDebits) {
            // This pane can appear in two different places in the flow,
            // and it might not have the consumer session created when launched from the
            // Link warmup sheet. Therefore, we need to fall back to the customer email.
            val consumerSession = consumerSessionProvider.provideConsumerSession()
            consumerSession?.emailAddress ?: manifest.accountholderCustomerEmailAddress
        } else {
            manifest.accountholderCustomerEmailAddress
        }

        return InitData(
            businessName = manifest.businessName,
            emailAddress = requireNotNull(email),
            initialInstitution = manifest.initialInstitution,
        )
    }

    private suspend fun startVerification(
        initData: InitData,
    ) {
        lookupConsumerAndStartVerification(
            email = initData.emailAddress,
            businessName = initData.businessName,
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
                val payload = buildPayload(consumerSession, initData.initialInstitution)
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

    private data class InitData(
        val businessName: String?,
        val emailAddress: String,
        val initialInstitution: FinancialConnectionsInstitution?,
    )

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
