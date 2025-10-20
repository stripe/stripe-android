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
import com.stripe.android.link.account.linkAccountUpdate
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.ConsentPresentation
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.utils.errorMessage
import com.stripe.android.model.ConsumerSessionRefresh
import com.stripe.android.ui.core.elements.OTPSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
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
    private val onVerificationSucceeded: (refresh: ConsumerSessionRefresh?) -> Unit,
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
            consentSection = (linkAccount.consentPresentation as? ConsentPresentation.Inline)?.consentSection
        )
    )
    val viewState: StateFlow<VerificationViewState> = _viewState

    val otpElement = OTPSpec.transform()

    private val otpCode: StateFlow<String?> =
        otpElement.otpCompleteFlow.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private var didSeeConsentSection = false

    init {
        setUp()
    }

    private fun setUp() {
        if (viewState.value.isProcessingWebAuth) {
            startWebVerification()
        } else if (linkAccount.accountStatus != AccountStatus.VerificationStarted) {
            startVerification()
        }

        viewModelScope.launch {
            otpCode.collect { code ->
                code?.let { onVerificationCodeEntered(code) }
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
            consentGranted = didSeeConsentSection.takeIf { it },
        ).fold(
            onSuccess = { account ->
                updateViewState { it.copy(isProcessing = false) }
                val isAuthenticationMode = linkLaunchMode is LinkLaunchMode.Authentication
                val completedAuthorizationConsent =
                    linkLaunchMode is LinkLaunchMode.Authorization &&
                        account.consentPresentation is ConsentPresentation.Inline
                if (isAuthenticationMode) {
                    dismissWithResult(
                        LinkActivityResult.Completed(
                            linkAccountUpdate = linkAccountManager.linkAccountUpdate,
                        )
                    )
                } else if (completedAuthorizationConsent) {
                    dismissWithResult(
                        LinkActivityResult.Completed(
                            linkAccountUpdate = linkAccountManager.linkAccountUpdate,
                            authorizationConsentGranted = true,
                        )
                    )
                } else {
                    onVerificationSucceeded(null)
                }
            },
            onFailure = {
                otpElement.controller.reset()
                onError(it)
            }
        )
    }

    private fun startVerification(isResend: Boolean = false) {
        updateViewState {
            it.copy(errorMessage = null)
        }

        viewModelScope.launch {
            val result = linkAccountManager.startVerification(isResendSmsCode = isResend)
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

    fun onBack() {
        clearError()
        onDismissClicked()
        linkEventsReporter.on2FACancel()
    }

    fun onChangeEmailButtonClicked() {
        clearError()
        onChangeEmailRequested()
        viewModelScope.launch {
            linkAccountManager.logOut()
        }
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
                        onVerificationSucceeded = parentComponent.viewModel::onVerificationSucceeded,
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
