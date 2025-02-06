package com.stripe.android.link.ui.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.getErrorMessage
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
    private val isDialog: Boolean,
    private val onVerificationSucceeded: (LinkAccount) -> Unit,
    private val onChangeEmailRequested: () -> Unit,
    private val onDismissClicked: () -> Unit,
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
            isDialog = isDialog
        )
    )
    val viewState: StateFlow<VerificationViewState> = _viewState

    val otpElement = OTPSpec.transform()

    private val otpCode: StateFlow<String?> =
        otpElement.otpCompleteFlow.stateIn(viewModelScope, SharingStarted.Lazily, null)

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

        linkAccountManager.confirmVerification(code).fold(
            onSuccess = { linkAccount ->
                updateViewState {
                    it.copy(isProcessing = false)
                }
                onVerificationSucceeded(linkAccount)
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
                    didSendNewCode = error == null,
                    errorMessage = error?.getErrorMessage()?.resolvableString,
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

    private fun onError(error: Throwable) = error.getErrorMessage().let { message ->
        logger.error("VerificationViewModel Error: ", error)

        updateViewState {
            it.copy(
                isProcessing = false,
                errorMessage = message.resolvableString,
            )
        }
    }

    private fun updateViewState(block: (VerificationViewState) -> VerificationViewState) {
        _viewState.update(block)
    }

    private val ErrorMessage.resolvableString: ResolvableString
        get() {
            return when (this) {
                is ErrorMessage.FromResources -> this.stringResId.resolvableString
                is ErrorMessage.Raw -> this.errorMessage.resolvableString
            }
        }

    companion object {
        fun factory(
            parentComponent: NativeLinkComponent,
            linkAccount: LinkAccount,
            isDialog: Boolean,
            onVerificationSucceeded: (LinkAccount) -> Unit,
            onChangeEmailClicked: () -> Unit = {},
            onDismissClicked: () -> Unit,
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    VerificationViewModel(
                        linkAccount = linkAccount,
                        linkAccountManager = parentComponent.linkAccountManager,
                        linkEventsReporter = parentComponent.linkEventsReporter,
                        logger = parentComponent.logger,
                        onVerificationSucceeded = onVerificationSucceeded,
                        onChangeEmailRequested = onChangeEmailClicked,
                        onDismissClicked = onDismissClicked,
                        isDialog = isDialog
                    )
                }
            }
        }
    }
}
