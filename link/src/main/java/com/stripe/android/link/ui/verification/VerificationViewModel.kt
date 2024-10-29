package com.stripe.android.link.ui.verification

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.AccountStatus
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
    private val linkAccountManager: LinkAccountManager,
    private val linkEventsReporter: LinkEventsReporter,
    private val logger: Logger,
    private val navController: NavHostController,
    private val goBack: (userInitiated: Boolean) -> Unit,
    private val navigateAndClearStack: (route: LinkScreen) -> Unit,
    private val dismissWithResult: (LinkActivityResult) -> Unit
) : ViewModel() {

    private val _viewState = MutableStateFlow(
        value = run {
            val linkAccount = linkAccountManager.linkAccount.value
            if (linkAccount == null) {
                dismissWithResult(LinkActivityResult.Failed(NoLinkAccountFoundForVerification()))
            }
            VerificationViewState(
                redactedPhoneNumber = linkAccount?.redactedPhoneNumber ?: "",
                email = linkAccount?.email ?: ""
            )
        }
    )
    val viewState: StateFlow<VerificationViewState> = _viewState

    /**
     * Callback when user has successfully verified their account. If not overridden, defaults to
     * navigating to the Wallet screen using [Navigator].
     */
    var onVerificationCompleted: () -> Unit = {
        navigateAndClearStack(LinkScreen.Wallet)
    }

    val otpElement = OTPSpec.transform()

    private val otpCode: StateFlow<String?> =
        otpElement.otpCompleteFlow.stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        init()
    }

    @VisibleForTesting
    internal fun init() {
        val linkAccount = linkAccountManager.linkAccount.value ?: return goBack(true)
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
        goBack(true)
        linkEventsReporter.on2FACancel()
        viewModelScope.launch {
            linkAccountManager.logout()
        }
    }

    fun onChangeEmailClicked() {
        clearError()
        navigateAndClearStack(LinkScreen.SignUp)
        viewModelScope.launch {
            linkAccountManager.logout()
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
                errorMessage = when (message) {
                    is ErrorMessage.FromResources -> message.stringResId.resolvableString
                    is ErrorMessage.Raw -> message.errorMessage.resolvableString
                },
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
            navController: NavHostController,
            goBack: (userInitiated: Boolean) -> Unit,
            navigateAndClearStack: (route: LinkScreen) -> Unit,
            dismissWithResult: (LinkActivityResult) -> Unit
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    VerificationViewModel(
                        linkAccountManager = parentComponent.linkAccountManager,
                        linkEventsReporter = parentComponent.linkEventsReporter,
                        logger = parentComponent.logger,
                        navController = navController,
                        goBack = goBack,
                        navigateAndClearStack = navigateAndClearStack,
                        dismissWithResult = dismissWithResult
                    )
                }
            }
        }
    }
}

internal class NoLinkAccountFoundForVerification : IllegalStateException("no link account found for verification")
