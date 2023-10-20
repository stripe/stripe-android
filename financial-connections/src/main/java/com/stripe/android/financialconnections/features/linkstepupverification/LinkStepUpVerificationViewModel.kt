package com.stripe.android.financialconnections.features.linkstepupverification

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
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationStepUpError
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationStepUpError.Error.ConsumerNotFoundError
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationStepUpError.Error.LookupConsumerSession
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationStepUpError.Error.MarkLinkVerifiedError
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationStepUpError.Error.StartVerificationError
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent.VerificationStepUpSuccess
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.logError
import com.stripe.android.financialconnections.domain.ConfirmVerification
import com.stripe.android.financialconnections.domain.GetCachedAccounts
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.LookupConsumerAndStartVerification
import com.stripe.android.financialconnections.domain.MarkLinkStepUpVerified
import com.stripe.android.financialconnections.domain.SelectNetworkedAccount
import com.stripe.android.financialconnections.domain.UpdateCachedAccounts
import com.stripe.android.financialconnections.domain.UpdateLocalManifest
import com.stripe.android.financialconnections.features.linkstepupverification.LinkStepUpVerificationState.Payload
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.Destination
import com.stripe.android.financialconnections.navigation.Destination.InstitutionPicker
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.VerificationType
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.OTPController
import com.stripe.android.uicore.elements.OTPElement
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class LinkStepUpVerificationViewModel @Inject constructor(
    initialState: LinkStepUpVerificationState,
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
) : MavericksViewModel<LinkStepUpVerificationState>(initialState) {

    init {
        logErrors()
        viewModelScope.launch { lookupAndStartVerification() }
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
        phoneNumber = consumerSession.redactedPhoneNumber,
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
    }

    private fun onOTPEntered(otp: String) = suspend {
        val payload = requireNotNull(awaitState().payload())
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

        private const val CLICKABLE_TEXT_RESEND_CODE = "resend_code"
        internal val PANE = Pane.LINK_STEP_UP_VERIFICATION
    }
}

internal data class LinkStepUpVerificationState(
    val payload: Async<Payload> = Uninitialized,
    val confirmVerification: Async<Unit> = Uninitialized,
    val resendOtp: Async<Unit> = Uninitialized,
) : MavericksState {

    data class Payload(
        val email: String,
        val phoneNumber: String,
        val otpElement: OTPElement,
        val consumerSessionClientSecret: String
    )
}
