package com.stripe.android.link.ui.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.injection.NonFallbackInjectable
import com.stripe.android.link.injection.NonFallbackInjector
import com.stripe.android.link.injection.SignedInViewModelSubcomponent
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.Navigator
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.getErrorMessage
import com.stripe.android.ui.core.elements.OTPSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

/**
 * ViewModel that handles user verification confirmation logic.
 */
internal class VerificationViewModel @Inject constructor(
    private val linkAccountManager: LinkAccountManager,
    private val navigator: Navigator,
    private val logger: Logger,
    val linkAccount: LinkAccount
) : ViewModel() {

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _errorMessage = MutableStateFlow<ErrorMessage?>(null)
    val errorMessage: StateFlow<ErrorMessage?> = _errorMessage

    /**
     * Callback when user has successfully verified their account. If not overridden, defaults to
     * navigating to the Wallet screen using [Navigator].
     */
    var onVerificationCompleted: () -> Unit = {
        navigator.navigateTo(LinkScreen.Wallet, clearBackStack = true)
    }

    init {
        if (linkAccount.accountStatus != AccountStatus.VerificationStarted) {
            startVerification()
        }
    }

    val otpElement = OTPSpec.transform()

    private val otpCode: StateFlow<String?> =
        otpElement.getFormFieldValueFlow().map { formFieldsList ->
            // formFieldsList contains only one element, for the OTP. Take the second value of
            // the pair, which is the FormFieldEntry containing the value entered by the user.
            formFieldsList.firstOrNull()?.second?.takeIf { it.isComplete }?.value
        }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        viewModelScope.launch {
            otpCode.collect { code ->
                code?.let { onVerificationCodeEntered(code) }
            }
        }
    }

    fun onVerificationCodeEntered(code: String) {
        _isProcessing.value = true
        clearError()

        viewModelScope.launch {
            linkAccountManager.confirmVerification(code).fold(
                onSuccess = {
                    _isProcessing.value = false
                    onVerificationCompleted()
                },
                onFailure = ::onError
            )
        }
    }

    fun startVerification() {
        clearError()

        viewModelScope.launch {
            linkAccountManager.startVerification().fold(
                onSuccess = {
                    logger.info("Verification code sent")
                },
                onFailure = ::onError
            )
        }
    }

    fun onBack() {
        clearError()
        navigator.onBack()
        linkAccountManager.logout()
    }

    fun onChangeEmailClicked() {
        clearError()
        navigator.navigateTo(LinkScreen.SignUp(), clearBackStack = true)
        linkAccountManager.logout()
    }

    private fun clearError() {
        _errorMessage.value = null
    }

    private fun onError(error: Throwable) = error.getErrorMessage().let {
        logger.error("Error: ", error)
        _isProcessing.value = false
        _errorMessage.value = it
    }

    internal class Factory(
        private val linkAccount: LinkAccount,
        private val injector: NonFallbackInjector
    ) : ViewModelProvider.Factory, NonFallbackInjectable {

        @Inject
        lateinit var subComponentBuilderProvider:
            Provider<SignedInViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            injector.inject(this)
            return subComponentBuilderProvider.get()
                .linkAccount(linkAccount)
                .build().verificationViewModel as T
        }
    }
}
