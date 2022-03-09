package com.stripe.android.link.ui.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.injection.LinkInjectable
import com.stripe.android.link.injection.LinkInjector
import com.stripe.android.link.injection.SignedInViewModelSubcomponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.Navigator
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

    fun onVerificationCodeEntered(code: String) {
        viewModelScope.launch {
            linkAccountManager.confirmVerification(code).fold(
                onSuccess = {
                    navigator.navigateTo(LinkScreen.Wallet)
                },
                onFailure = ::onError
            )
        }
    }

    fun onResendCodeClicked() {
        viewModelScope.launch {
            linkAccountManager.startVerification().fold(
                onSuccess = {
                    logger.info("Verification code sent")
                },
                onFailure = ::onError
            )
        }
    }

    private fun onError(error: Throwable) {
        logger.error(error.localizedMessage ?: "Internal error.")
        // TODO(brnunes-stripe): Add localized error messages, show them in UI.
    }

    internal class Factory(
        private val linkAccount: LinkAccount,
        private val injector: LinkInjector
    ) : ViewModelProvider.Factory, LinkInjectable {

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
