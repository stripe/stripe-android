package com.stripe.android.link.ui.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.linkAccountUpdate
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.ConsentPresentation
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.utils.errorMessage
import com.stripe.android.ui.core.elements.OTPSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel that handles user verification confirmation logic.
 */
internal class VerificationViewModel @Inject constructor(
    private val linkAccount: LinkAccount,
    private val linkAccountManager: LinkAccountManager,
    private val linkEventsReporter: LinkEventsReporter,
    private val logger: Logger,
    private val linkLaunchMode: LinkLaunchMode,
    private val isDialog: Boolean,
    private val onVerificationSucceeded: () -> Unit,
    private val onChangeEmailRequested: () -> Unit,
    private val onDismissClicked: () -> Unit,
    private val dismissWithResult: (LinkActivityResult) -> Unit,
) : ViewModel() {

    private val _viewState = MutableStateFlow(
        value = VerificationViewState(
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
        if (linkAccount.accountStatus != AccountStatus.VerificationStarted) {
            startVerification()
        }

        viewModelScope.launch {
            otpCode.collect { code ->
                code?.let { onVerificationCodeEntered(code) }
            }
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
                    onVerificationSucceeded()
                }
            },
            onFailure = {
                otpElement.controller.reset()
                onError(it)
            }
        )
    }

    private fun startVerification() {
        updateViewState {
            it.copy(errorMessage = null)
        }

        viewModelScope.launch {
            val result = linkAccountManager.startVerification()
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

    fun resendCode() {
        updateViewState { it.copy(isSendingNewCode = true) }
        startVerification()
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
            onVerificationSucceeded: () -> Unit,
            onChangeEmailClicked: () -> Unit,
            onDismissClicked: () -> Unit,
            dismissWithResult: (LinkActivityResult) -> Unit,
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    VerificationViewModel(
                        linkAccount = linkAccount,
                        linkAccountManager = parentComponent.linkAccountManager,
                        linkEventsReporter = parentComponent.linkEventsReporter,
                        logger = parentComponent.logger,
                        linkLaunchMode = parentComponent.linkLaunchMode,
                        onVerificationSucceeded = onVerificationSucceeded,
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
