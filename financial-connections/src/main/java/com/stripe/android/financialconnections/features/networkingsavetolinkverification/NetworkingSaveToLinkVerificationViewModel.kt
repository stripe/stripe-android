package com.stripe.android.financialconnections.features.networkingsavetolinkverification

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.PaneLoaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationError
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationError.Error.ConfirmVerificationSessionError
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationError.Error.StartVerificationSessionError
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationSuccess
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.ConfirmVerification
import com.stripe.android.financialconnections.domain.ConfirmVerification.OTPError
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetOrFetchSync
import com.stripe.android.financialconnections.domain.MarkLinkVerified
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.SaveAccountToLink
import com.stripe.android.financialconnections.domain.StartVerification
import com.stripe.android.financialconnections.features.common.isDataFlow
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.PaymentAccountParams
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.financialconnections.repository.AttachedPaymentAccountRepository
import com.stripe.android.financialconnections.repository.ConsumerSessionProvider
import com.stripe.android.financialconnections.utils.error
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.uicore.elements.OTPElement
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.stripe.android.financialconnections.navigation.Destination.Success as SuccessDestination

internal class NetworkingSaveToLinkVerificationViewModel @AssistedInject constructor(
    @Assisted initialState: NetworkingSaveToLinkVerificationState,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val consumerSessionProvider: ConsumerSessionProvider,
    private val startVerification: StartVerification,
    private val getOrFetchSync: GetOrFetchSync,
    private val confirmVerification: ConfirmVerification,
    private val attachedPaymentAccountRepository: AttachedPaymentAccountRepository,
    private val markLinkVerified: MarkLinkVerified,
    private val getCachedAccounts: GetCachedAccounts,
    private val saveAccountToLink: SaveAccountToLink,
    private val navigationManager: NavigationManager,
    private val logger: Logger
) : FinancialConnectionsViewModel<NetworkingSaveToLinkVerificationState>(initialState, nativeAuthFlowCoordinator) {

    init {
        logErrors()
        suspend {
            val consumerSession = requireNotNull(consumerSessionProvider.provideConsumerSession())
            // If we automatically moved to this pane due to prefilled email, we should show the "Not now" button.
            val showNotNowButton = getOrFetchSync().manifest.accountholderCustomerEmailAddress != null
            runCatching {
                startVerification.sms(consumerSession.clientSecret)
            }.onFailure {
                eventTracker.track(VerificationError(PANE, StartVerificationSessionError))
            }.getOrThrow()
            eventTracker.track(PaneLoaded(PANE))
            NetworkingSaveToLinkVerificationState.Payload(
                showNotNowButton = showNotNowButton,
                email = consumerSession.emailAddress,
                phoneNumber = consumerSession.phoneNumber,
                consumerSessionClientSecret = consumerSession.clientSecret,
                otpElement = OTPElement(
                    IdentifierSpec.Generic("otp"),
                    OTPController()
                )
            )
        }.execute { copy(payload = it) }
    }

    override fun updateTopAppBar(state: NetworkingSaveToLinkVerificationState): TopAppBarStateUpdate {
        return TopAppBarStateUpdate(
            pane = PANE,
            allowBackNavigation = true,
            error = state.payload.error,
        )
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
                eventTracker.logError(
                    extraMessage = "Error fetching payload",
                    error = error,
                    logger = logger,
                    pane = PANE
                )
            },
        )
        onAsync(
            NetworkingSaveToLinkVerificationState::confirmVerification,
            onSuccess = {
                navigationManager.tryNavigateTo(SuccessDestination(referrer = PANE))
            },
            onFail = { error ->
                eventTracker.logError(
                    extraMessage = "Error confirming verification",
                    error = error,
                    logger = logger,
                    pane = PANE
                )
                if (error !is OTPError) {
                    navigationManager.tryNavigateTo(SuccessDestination(referrer = PANE))
                }
            },
        )
    }

    private fun onOTPEntered(otp: String) = suspend {
        val payload = requireNotNull(stateFlow.value.payload())

        runCatching {
            confirmVerification.sms(
                consumerSessionClientSecret = payload.consumerSessionClientSecret,
                verificationCode = otp
            )

            val accounts = getCachedAccounts()
            if (accounts.isEmpty()) {
                val attachedAccount = attachedPaymentAccountRepository.get()?.attachedPaymentAccount
                require(
                    value = attachedAccount is PaymentAccountParams.BankAccount,
                    lazyMessage = { "An already attached account is required when no accounts cached" }
                )
            }
            val manifest = getOrFetchSync().manifest

            saveAccountToLink.existing(
                consumerSessionClientSecret = payload.consumerSessionClientSecret,
                selectedAccounts = accounts,
                shouldPollAccountNumbers = manifest.isDataFlow,
                isNetworkingRelinkSession = false,
            )
        }
            .onSuccess { eventTracker.track(VerificationSuccess(PANE)) }
            .onFailure {
                eventTracker.track(VerificationError(PANE, ConfirmVerificationSessionError))
            }.getOrThrow()

        // Mark link verified (ignore its result).
        kotlin.runCatching { markLinkVerified() }
        Unit
    }.execute { copy(confirmVerification = it) }

    fun onSkipClick() {
        navigationManager.tryNavigateTo(SuccessDestination(referrer = PANE))
    }

    @AssistedFactory
    interface Factory {
        fun create(initialState: NetworkingSaveToLinkVerificationState): NetworkingSaveToLinkVerificationViewModel
    }

    companion object {

        internal val PANE = Pane.NETWORKING_SAVE_TO_LINK_VERIFICATION

        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    parentComponent.networkingSaveToLinkVerificationViewModelFactory.create(
                        NetworkingSaveToLinkVerificationState()
                    )
                }
            }
    }
}

internal data class NetworkingSaveToLinkVerificationState(
    val payload: Async<Payload> = Uninitialized,
    val confirmVerification: Async<Unit> = Uninitialized
) {

    data class Payload(
        val showNotNowButton: Boolean,
        val email: String,
        val phoneNumber: String,
        val otpElement: OTPElement,
        val consumerSessionClientSecret: String
    )
}
