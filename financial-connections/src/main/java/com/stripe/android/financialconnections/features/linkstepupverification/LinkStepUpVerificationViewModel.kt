package com.stripe.android.financialconnections.features.linkstepupverification

import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationStepUpError
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationStepUpError.Error.ConsumerNotFoundError
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationStepUpError.Error.LookupConsumerSession
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationStepUpError.Error.MarkLinkVerifiedError
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationStepUpError.Error.StartVerificationError
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationStepUpSuccess
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.di.FinancialConnectionsSheetNativeComponent
import com.stripe.android.financialconnections.domain.ConfirmVerification
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.LookupConsumerAndStartVerification
import com.stripe.android.financialconnections.domain.MarkLinkStepUpVerified
import com.stripe.android.financialconnections.domain.NativeAuthFlowCoordinator
import com.stripe.android.financialconnections.domain.SelectNetworkedAccount
import com.stripe.android.financialconnections.domain.UpdateCachedAccounts
import com.stripe.android.financialconnections.domain.UpdateLocalManifest
import com.stripe.android.financialconnections.features.linkstepupverification.LinkStepUpVerificationState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.Destination.InstitutionPicker
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.topappbar.TopAppBarStateUpdate
import com.stripe.android.financialconnections.presentation.Async
import com.stripe.android.financialconnections.presentation.Async.Fail
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Success
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.presentation.FinancialConnectionsViewModel
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.VerificationType
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.uicore.elements.OTPElement
import getRedactedPhoneNumber
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class LinkStepUpVerificationViewModel @Inject constructor(
    initialState: LinkStepUpVerificationState,
    nativeAuthFlowCoordinator: NativeAuthFlowCoordinator,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    private val getManifest: GetManifest,
    private val lookupConsumerAndStartVerification: LookupConsumerAndStartVerification,
    private val confirmVerification: ConfirmVerification,
    private val selectNetworkedAccount: SelectNetworkedAccount,
    private val getCachedAccounts: GetCachedAccounts,
    private val updateLocalManifest: UpdateLocalManifest,
    private val markLinkStepUpVerified: MarkLinkStepUpVerified,
    private val updateCachedAccounts: UpdateCachedAccounts,
    private val navigationManager: NavigationManager,
    private val logger: Logger
) : FinancialConnectionsViewModel<LinkStepUpVerificationState>(initialState, nativeAuthFlowCoordinator) {

    init {
        logErrors()
        viewModelScope.launch { lookupAndStartVerification() }
    }

    override fun updateTopAppBar(state: LinkStepUpVerificationState): TopAppBarStateUpdate {
        return TopAppBarStateUpdate(
            pane = PANE,
            allowBackNavigation = false,
        )
    }

    private suspend fun lookupAndStartVerification() = runCatching {
        getManifest().also { requireNotNull(it.accountholderCustomerEmailAddress) }
    }
        .onFailure { setState { copy(payload = Fail(it)) } }
        .onSuccess { manifest ->
            setState { copy(payload = Loading()) }
            lookupConsumerAndStartVerification(
                email = requireNotNull(manifest.accountholderCustomerEmailAddress),
                businessName = manifest.businessName,
                verificationType = VerificationType.EMAIL,
                onConsumerNotFound = {
                    eventTracker.track(VerificationStepUpError(PANE, ConsumerNotFoundError))
                    navigationManager.tryNavigateTo(InstitutionPicker(referrer = PANE))
                },
                onLookupError = { error ->
                    eventTracker.track(VerificationStepUpError(PANE, LookupConsumerSession))
                    setState { copy(payload = Fail(error)) }
                },
                onStartVerification = { /* no-op */ },
                onVerificationStarted = { consumerSession ->
                    val payload = buildPayload(consumerSession)
                    setState { copy(payload = Success(payload)) }
                },
                onStartVerificationError = { error ->
                    eventTracker.track(VerificationStepUpError(PANE, StartVerificationError))
                    setState { copy(payload = Fail(error)) }
                }
            )
        }

    private fun buildPayload(consumerSession: ConsumerSession) = Payload(
        email = consumerSession.emailAddress,
        phoneNumber = consumerSession.getRedactedPhoneNumber(),
        consumerSessionClientSecret = consumerSession.clientSecret,
        otpElement = OTPElement(
            IdentifierSpec.Generic("otp"),
            OTPController()
        )
    )

    private fun logErrors() {
        onAsync(
            LinkStepUpVerificationState::payload,
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
            LinkStepUpVerificationState::confirmVerification,
            onFail = { error ->
                eventTracker.logError(
                    extraMessage = "Error confirming verification",
                    error = error,
                    logger = logger,
                    pane = PANE
                )
            },
        )
    }

    private fun onOTPEntered(otp: String) = suspend {
        val payload = requireNotNull(stateFlow.value.payload())
        // Confirm email.
        confirmVerification.email(
            consumerSessionClientSecret = payload.consumerSessionClientSecret,
            verificationCode = otp
        )

        // Get accounts selected in networked accounts picker.
        val selectedAccount = getCachedAccounts().first()

        // Mark session as verified.
        runCatching { markLinkStepUpVerified() }
            .onFailure {
                eventTracker.track(
                    VerificationStepUpError(
                        PANE,
                        MarkLinkVerifiedError
                    )
                )
            }
            .onSuccess { eventTracker.track(VerificationStepUpSuccess(PANE)) }
            .getOrThrow()

        // Mark networked account as selected.
        val activeInstitution = selectNetworkedAccount(
            consumerSessionClientSecret = payload.consumerSessionClientSecret,
            selectedAccountId = selectedAccount.id
        )

        // Updates manifest active institution after account networked.
        updateLocalManifest { it.copy(activeInstitution = activeInstitution.data.firstOrNull()) }
        // Updates cached accounts with the one selected.
        updateCachedAccounts { listOf(selectedAccount) }
        navigationManager.tryNavigateTo(Destination.Success(referrer = PANE))
    }.execute { copy(confirmVerification = it) }

    fun onClickableTextClick(text: String) {
        when (text) {
            CLICKABLE_TEXT_RESEND_CODE -> viewModelScope.launch { onResendOtp() }
            else -> logger.error("Unknown clicked text $text")
        }
    }

    private suspend fun onResendOtp() = runCatching {
        getManifest().also { requireNotNull(it.accountholderCustomerEmailAddress) }
    }
        .onFailure { setState { copy(resendOtp = Fail(it)) } }
        .onSuccess { manifest ->
            setState { copy(resendOtp = Loading()) }
            lookupConsumerAndStartVerification(
                email = requireNotNull(manifest.accountholderCustomerEmailAddress),
                businessName = manifest.businessName,
                verificationType = VerificationType.EMAIL,
                onConsumerNotFound = {
                    eventTracker.track(VerificationStepUpError(PANE, ConsumerNotFoundError))
                    navigationManager.tryNavigateTo(InstitutionPicker(referrer = PANE))
                },
                onLookupError = { error ->
                    eventTracker.track(VerificationStepUpError(PANE, LookupConsumerSession))
                    setState { copy(resendOtp = Fail(error)) }
                },
                onStartVerification = { /* no-op */ },
                onVerificationStarted = { setState { copy(resendOtp = Success(Unit)) } },
                onStartVerificationError = { error ->
                    eventTracker.track(VerificationStepUpError(PANE, StartVerificationError))
                    setState { copy(resendOtp = Fail(error)) }
                }
            )
        }

    companion object {

        fun factory(parentComponent: FinancialConnectionsSheetNativeComponent): Factory =
            viewModelFactory {
                initializer {
                    parentComponent
                        .linkStepUpVerificationSubcomponent
                        .create(LinkStepUpVerificationState())
                        .viewModel
                }
            }

        private const val CLICKABLE_TEXT_RESEND_CODE = "resend_code"
        internal val PANE = Pane.LINK_STEP_UP_VERIFICATION
    }
}

internal data class LinkStepUpVerificationState(
    val payload: Async<Payload> = Uninitialized,
    val confirmVerification: Async<Unit> = Uninitialized,
    val resendOtp: Async<Unit> = Uninitialized,
) {

    val submitLoading: Boolean
        get() = confirmVerification is Loading || resendOtp is Loading
    val submitError: Throwable?
        get() = (confirmVerification as? Fail)?.error ?: (resendOtp as? Fail)?.error

    data class Payload(
        val email: String,
        val phoneNumber: String,
        val otpElement: OTPElement,
        val consumerSessionClientSecret: String
    )
}
