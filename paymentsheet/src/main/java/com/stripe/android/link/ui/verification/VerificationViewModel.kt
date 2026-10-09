package com.stripe.android.link.ui.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.WebLinkAuthChannel
import com.stripe.android.link.WebLinkAuthResult
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.LinkAuthCapabilities
import com.stripe.android.link.account.linkAccountUpdate
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.effectiveLinkBrand
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.ConsentPresentation
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.utils.errorMessage
import com.stripe.android.model.ConsumerSessionRefresh
import com.stripe.android.model.LinkBrand
import com.stripe.android.model.VerificationType
import com.stripe.android.uicore.elements.OTPElementFactory
import com.stripe.android.uicore.elements.PhoneNumberController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel that handles user verification confirmation logic.
 */
internal class VerificationViewModel @Inject constructor(
    private val linkAccount: LinkAccount,
    private val linkAccountHolder: LinkAccountHolder,
    private val linkAccountManager: LinkAccountManager,
    private val linkEventsReporter: LinkEventsReporter,
    private val logger: Logger,
    private val linkLaunchMode: LinkLaunchMode,
    private val webLinkAuthChannel: WebLinkAuthChannel,
    private val isDialog: Boolean,
    private val linkBrand: LinkBrand,
    private val enableMfaAuthFlow: Boolean,
    private val onVerificationSucceeded: (refresh: ConsumerSessionRefresh?) -> Unit,
    private val setScreenBackHandler: (handler: (() -> Unit)?) -> Unit,
    private val onChangeEmailRequested: () -> Unit,
    private val onDismissClicked: () -> Unit,
    private val dismissWithResult: (LinkActivityResult) -> Unit,
) : ViewModel() {

    private val _viewState = MutableStateFlow(
        value = VerificationViewState(
            isProcessingWebAuth = linkAccount.webviewOpenUrl != null,
            redactedPhoneNumber = linkAccount.redactedPhoneNumber,
            email = linkAccount.email,
            isProcessing = false,
            requestFocus = true,
            errorMessage = null,
            isSendingNewCode = false,
            didSendNewCode = false,
            defaultPayment = null,
            isDialog = isDialog,
            allowLogout = !isDialog || linkLaunchMode is LinkLaunchMode.PaymentMethodSelection,
            consentSection = (linkAccount.consentPresentation as? ConsentPresentation.Inline)?.consentSection,
            linkBrand = linkBrand,
            authFlow = null,
        )
    )
    val viewState: StateFlow<VerificationViewState> = _viewState

    val otpElement = OTPElementFactory.create()

    val phoneNumberController = PhoneNumberController.createPhoneNumberController(
        initiallySelectedCountryCode = linkAccount.phoneNumberCountry,
    )

    private var didSeeConsentSection = false

    /**
     * Drives email OTP, phone-match and multi-factor verification. Null when only SMS OTP is supported.
     */
    private val authFlow: LinkAuthFlow? = if (
        enableMfaAuthFlow && !viewState.value.isProcessingWebAuth
    ) {
        LinkAuthFlow(
            linkAccountManager = linkAccountManager,
            linkEventsReporter = linkEventsReporter,
            capabilities = LinkAuthCapabilities.supportedVerificationTypes(enableMfaAuthFlow),
            consentGranted = { didSeeConsentSection.takeIf { it } },
            scope = viewModelScope,
            now = System::currentTimeMillis,
            onFinish = ::onAuthFlowFinished,
        )
    } else {
        null
    }

    private var lastInputRevision: Int? = null

    private val otpCode: StateFlow<String?> =
        otpElement.otpCompleteFlow.stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        setUp()
    }

    private fun setUp() {
        val authFlow = authFlow
        if (viewState.value.isProcessingWebAuth) {
            startWebVerification()
        } else if (authFlow != null) {
            viewModelScope.launch {
                authFlow.state.collect(::onAuthFlowStateChanged)
            }
            if (!isDialog) {
                viewModelScope.launch {
                    authFlow.state.map { it.canGoBack }.distinctUntilChanged().collect { canGoBack ->
                        setScreenBackHandler(if (canGoBack) ({ onNavigateBack() }) else null)
                    }
                }
            }
            authFlow.start()
        } else if (linkAccount.accountStatus != AccountStatus.VerificationStarted) {
            startVerification()
        }

        viewModelScope.launch {
            if (authFlow != null) {
                // Collect the raw flow so an identical code entered again (e.g. after a failed confirm) is resubmitted.
                otpElement.otpCompleteFlow.collect { code -> authFlow.confirm(code) }
            } else {
                otpCode.collect { code ->
                    code?.let { onVerificationCodeEntered(code) }
                }
            }
        }

        viewModelScope.launch {
            handleWebAuthResults()
        }
    }

    suspend fun onVerificationCodeEntered(code: String) {
        updateViewState {
            it.copy(
                isProcessing = true,
                errorMessage = null,
            )
        }

        linkAccountManager.confirmVerification(
            code = code,
            type = VerificationType.SMS,
            consentGranted = didSeeConsentSection.takeIf { it },
        ).fold(
            onSuccess = { account ->
                updateViewState { it.copy(isProcessing = false) }
                onAccountVerified(account)
            },
            onFailure = {
                otpElement.controller.reset()
                onError(it)
            }
        )
    }

    private fun onAccountVerified(account: LinkAccount) {
        val isAuthenticationMode = linkLaunchMode is LinkLaunchMode.Authentication
        val completedAuthorization =
            linkLaunchMode is LinkLaunchMode.Authorization &&
                (
                    account.consentPresentation == null ||
                        account.consentPresentation is ConsentPresentation.Inline
                    )

        if (isAuthenticationMode) {
            dismissWithResult(
                LinkActivityResult.Completed(
                    linkAccountUpdate = linkAccountManager.linkAccountUpdate,
                )
            )
        } else if (completedAuthorization) {
            dismissWithResult(
                LinkActivityResult.Completed(
                    linkAccountUpdate = linkAccountManager.linkAccountUpdate,
                    authorizationConsentGranted = true,
                )
            )
        } else {
            onVerificationSucceeded(null)
        }
    }

    private fun onAuthFlowStateChanged(state: LinkAuthFlowState) {
        val isNewInput = state.inputRevision != lastInputRevision
        lastInputRevision = state.inputRevision
        if (isNewInput) {
            otpElement.controller.reset()
        }
        updateViewState {
            it.copy(
                isProcessing = state.isLoading && !state.isResending,
                isSendingNewCode = state.isResending,
                errorMessage = state.errorMessage,
                requestFocus = it.requestFocus || (isNewInput && state.screen == LinkAuthFlowState.Screen.Otp),
                authFlow = VerificationViewState.AuthFlowViewState(
                    screen = state.screen,
                    recipient = state.recipient,
                    canGoBack = state.canGoBack,
                    codeEntryEnabled = state.canSubmitCode,
                    actions = state.actions,
                    canResend = state.canResend,
                    isResending = state.isResending,
                    resendSecondsRemaining = state.resendSecondsRemaining,
                    phoneNumberLastTwoDigits = state.phoneNumberLastTwoDigits,
                ),
            )
        }
    }

    private fun onAuthFlowFinished(result: LinkAuthFlowResult) {
        when (result) {
            LinkAuthFlowResult.Completed -> {
                val account = linkAccountManager.linkAccountInfo.value.account
                if (account != null) {
                    onAccountVerified(account)
                } else {
                    onDismissClicked()
                }
            }
            LinkAuthFlowResult.Canceled -> onDismissClicked()
            LinkAuthFlowResult.SwitchAccount -> onChangeEmailRequested()
            LinkAuthFlowResult.RequiresWebAuth -> {
                updateViewState { it.copy(isProcessingWebAuth = true) }
                startWebVerification()
            }
            is LinkAuthFlowResult.Failed -> {
                logger.error("VerificationViewModel Error: ", result.error)
                dismissWithResult(
                    LinkActivityResult.Failed(
                        error = result.error,
                        linkAccountUpdate = LinkAccountUpdate.None
                    )
                )
            }
        }
    }

    private fun startVerification(isResend: Boolean = false) {
        updateViewState {
            it.copy(errorMessage = null)
        }

        viewModelScope.launch {
            val result = linkAccountManager.startVerification(
                type = VerificationType.SMS,
                accountPhoneNumber = null,
                isResend = isResend,
            )
            val error = result.exceptionOrNull()

            updateViewState {
                it.copy(
                    isSendingNewCode = false,
                    didSendNewCode = it.isSendingNewCode && error == null,
                    errorMessage = error?.errorMessage,
                )
            }
        }
    }

    private fun startWebVerification() {
        viewModelScope.launch {
            // The web auth URL is single use, so if the web auth URL has already been consumed,
            // refresh the consumer session to get a fresh auth URL.
            val updatedLinkAccountResult = linkAccount
                .takeIf { !it.viewedWebviewOpenUrl }
                ?.let { Result.success(it) }
                ?: linkAccountManager.refreshConsumer()
                    // Get the updated account after refreshing the consumer session.
                    .mapCatching { checkNotNull(linkAccountManager.linkAccountInfo.value.account) }
            updatedLinkAccountResult.fold(
                onSuccess = { account ->
                    // If we don't have a URL here, something went wrong upstream.
                    // Cancel so user can try again.
                    if (account.webviewOpenUrl == null) {
                        dismissWithResult(
                            LinkActivityResult.Canceled(linkAccountUpdate = linkAccountManager.linkAccountUpdate)
                        )
                        return@fold
                    }
                    // Mark the URL as viewed so we don't try to reuse it.
                    linkAccountHolder.set(
                        LinkAccountUpdate.Value(account = account.copy(viewedWebviewOpenUrl = true))
                    )
                    webLinkAuthChannel.requests.emit(account.webviewOpenUrl)
                },
                onFailure = { error ->
                    dismissWithResult(
                        LinkActivityResult.Failed(
                            error = error,
                            linkAccountUpdate = LinkAccountUpdate.None
                        )
                    )
                }
            )
        }
    }

    fun resendCode() {
        authFlow?.let {
            it.resend()
            return
        }
        linkEventsReporter.on2FAResendCode(verificationType = "SMS")
        updateViewState { it.copy(isSendingNewCode = true) }
        startVerification(isResend = true)
    }

    fun didShowCodeSentNotification() {
        updateViewState {
            it.copy(didSendNewCode = false)
        }
    }

    fun onConsentShown() {
        didSeeConsentSection = true
    }

    fun onEmailCodeClicked() {
        authFlow?.sendToEmail()
    }

    fun onPhoneNumberSubmitted() {
        val phoneNumber = phoneNumberController.getE164PhoneNumber(phoneNumberController.fieldValue.value)
        authFlow?.submitPhoneNumber(phoneNumber)
    }

    /**
     * Returns to the previous verification step, if there is one.
     *
     * @return whether the back press was handled.
     */
    fun onNavigateBack(): Boolean {
        val authFlow = authFlow ?: return false
        if (!authFlow.state.value.canGoBack) return false
        authFlow.goBack()
        return true
    }

    fun onBack() {
        clearError()
        authFlow?.let {
            it.cancel(switchAccount = false)
            return
        }
        onDismissClicked()
        linkEventsReporter.on2FACancel()
    }

    fun onChangeEmailButtonClicked() {
        clearError()
        authFlow?.let {
            it.cancel(switchAccount = true)
            return
        }
        onChangeEmailRequested()
    }

    fun onFocusRequested() {
        updateViewState {
            it.copy(requestFocus = false)
        }
    }

    // This probably belongs in `LinkActivityViewModel` but we'd have to refactor
    // verification cancellation/dismissal first.
    private suspend fun handleWebAuthResults() {
        webLinkAuthChannel.results.collectLatest { result ->
            when (result) {
                WebLinkAuthResult.Completed -> {
                    linkAccountManager.refreshConsumer().fold(
                        onSuccess = onVerificationSucceeded,
                        onFailure = {
                            dismissWithResult(
                                LinkActivityResult.Failed(
                                    error = it,
                                    linkAccountUpdate = LinkAccountUpdate.None
                                )
                            )
                        }
                    )
                }
                WebLinkAuthResult.Canceled -> {
                    onDismissClicked()
                }
                is WebLinkAuthResult.Failure -> {
                    dismissWithResult(
                        LinkActivityResult.Failed(
                            error = result.error,
                            linkAccountUpdate = LinkAccountUpdate.None
                        )
                    )
                }
            }
        }
    }

    private fun clearError() {
        updateViewState {
            it.copy(errorMessage = null)
        }
    }

    private fun onError(error: Throwable) = error.errorMessage.let { message ->
        logger.error("VerificationViewModel Error: ", error)

        updateViewState {
            it.copy(
                isProcessing = false,
                errorMessage = message,
            )
        }
    }

    override fun onCleared() {
        if (authFlow != null && !isDialog) {
            setScreenBackHandler(null)
        }
        super.onCleared()
    }

    private fun updateViewState(block: (VerificationViewState) -> VerificationViewState) {
        _viewState.update(block)
    }

    companion object {
        fun factory(
            parentComponent: NativeLinkComponent,
            linkAccount: LinkAccount,
            isDialog: Boolean,
            onChangeEmailClicked: () -> Unit,
            onDismissClicked: () -> Unit,
            dismissWithResult: (LinkActivityResult) -> Unit,
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    VerificationViewModel(
                        linkAccount = linkAccount,
                        linkAccountHolder = parentComponent.linkAccountHolder,
                        linkAccountManager = parentComponent.linkAccountManager,
                        linkEventsReporter = parentComponent.linkEventsReporter,
                        logger = parentComponent.logger,
                        linkLaunchMode = parentComponent.linkLaunchMode,
                        webLinkAuthChannel = parentComponent.webLinkAuthChannel,
                        linkBrand = parentComponent.configuration.effectiveLinkBrand(linkAccount),
                        enableMfaAuthFlow = parentComponent.configuration.enableMfaAuthFlow,
                        onVerificationSucceeded = parentComponent.viewModel::onVerificationSucceeded,
                        setScreenBackHandler = parentComponent.viewModel::setScreenBackHandler,
                        onChangeEmailRequested = onChangeEmailClicked,
                        onDismissClicked = onDismissClicked,
                        isDialog = isDialog,
                        dismissWithResult = dismissWithResult
                    )
                }
            }
        }
    }
}
