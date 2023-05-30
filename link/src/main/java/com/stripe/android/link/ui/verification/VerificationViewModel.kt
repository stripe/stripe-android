package com.stripe.android.link.ui.verification

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.NonFallbackInjectable
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.Navigator
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

internal data class VerificationViewState(
    val isProcessing: Boolean = false,
    val requestFocus: Boolean = true,
    val errorMessage: ErrorMessage? = null,
    val isSendingNewCode: Boolean = false,
    val didSendNewCode: Boolean = false,
)

/**
 * ViewModel that handles user verification confirmation logic.
 */
@Suppress("TooManyFunctions")
internal class VerificationViewModel @Inject constructor(
    private val linkAccountManager: LinkAccountManager,
    private val linkEventsReporter: LinkEventsReporter,
    private val navigator: Navigator,
    private val logger: Logger
) : ViewModel() {
    lateinit var linkAccount: LinkAccount

    private val _viewState = MutableStateFlow(VerificationViewState())
    val viewState: StateFlow<VerificationViewState> = _viewState

    /**
     * Callback when user has successfully verified their account. If not overridden, defaults to
     * navigating to the Wallet screen using [Navigator].
     */
    var onVerificationCompleted: () -> Unit = {
        navigator.navigateTo(LinkScreen.Wallet, clearBackStack = true)
    }

    val otpElement = OTPSpec.transform()

    private val otpCode: StateFlow<String?> =
        otpElement.otpCompleteFlow.stateIn(viewModelScope, SharingStarted.Lazily, null)

    @VisibleForTesting
    internal fun init(linkAccount: LinkAccount) {
        this.linkAccount = linkAccount
        if (linkAccount.accountStatus != AccountStatus.VerificationStarted) {
            startVerification()
        }

        linkEventsReporter.on2FAStart()

        viewModelScope.launch {
            otpCode.collect { code ->
                code?.let { onVerificationCodeEntered(code) }
            }
        }
    }

    fun onVerificationCodeEntered(code: String) {
        updateViewState {
            it.copy(
                isProcessing = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            linkAccountManager.confirmVerification(code).fold(
                onSuccess = {
                    updateViewState {
                        it.copy(isProcessing = false)
                    }

                    linkEventsReporter.on2FAComplete()
                    onVerificationCompleted()
                },
                onFailure = {
                    linkEventsReporter.on2FAFailure()
                    for (i in 0 until otpElement.controller.otpLength) {
                        otpElement.controller.onValueChanged(i, "")
                    }
                    onError(it)
                }
            )
        }
    }

    fun startVerification() {
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
                    errorMessage = error?.getErrorMessage(),
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
        navigator.onBack(userInitiated = true)
        linkEventsReporter.on2FACancel()
        linkAccountManager.logout()
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
        logger.error("Error: ", error)

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

    internal class Factory(
        private val account: LinkAccount,
        private val injector: NonFallbackInjector
    ) : ViewModelProvider.Factory, NonFallbackInjectable {

        @Inject
        lateinit var viewModel: VerificationViewModel

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            injector.inject(this)
            return viewModel.apply {
                init(account)
            } as T
        }
    }
}
