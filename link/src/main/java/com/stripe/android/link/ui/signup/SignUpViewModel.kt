package com.stripe.android.link.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.injection.LinkViewModelSubcomponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.Navigator
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.SectionFieldElement
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

/**
 * ViewModel that handles user sign up logic.
 */
internal class SignUpViewModel @Inject constructor(
    args: LinkActivityContract.Args,
    private val linkAccountManager: LinkAccountManager,
    private val navigator: Navigator,
    private val logger: Logger
) : ViewModel() {
    val merchantName: String = args.merchantName

    val emailElement: SectionFieldElement = EmailSpec.transform(args.customerEmail)

    /**
     * Emits the email entered in the form if valid, null otherwise.
     */
    private val consumerEmail: StateFlow<String?> =
        emailElement.getFormFieldValueFlow().map { formFieldsList ->
            // formFieldsList contains only one element, for the email. Take the second value of
            // the pair, which is the FormFieldEntry containing the value entered by the user.
            formFieldsList.firstOrNull()?.second?.takeIf { it.isComplete }?.value
        }.stateIn(viewModelScope, SharingStarted.Lazily, args.customerEmail)

    private val _signUpStatus = MutableStateFlow(SignUpState.InputtingEmail)
    val signUpState: StateFlow<SignUpState> = _signUpStatus

    /**
     * Holds a Job that looks up the email after a delay, so that we can cancel it if the user is
     * still typing.
     */
    private var lookupJob: Job? = null

    init {
        viewModelScope.launch {
            consumerEmail.collect { email ->
                // The first emitted value is the one provided in the arguments, and shouldn't
                // trigger a lookup because it was already done on the loading screen.
                if (email == args.customerEmail && lookupJob == null) {
                    // If it's a valid email, collect phone number
                    if (email != null) {
                        _signUpStatus.value = SignUpState.InputtingPhone
                    }
                    return@collect
                }

                lookupJob?.cancel()

                if (email != null) {
                    lookupJob = launch {
                        delay(LOOKUP_DEBOUNCE_MS)
                        if (isActive) {
                            _signUpStatus.value = SignUpState.VerifyingEmail
                            lookupConsumerEmail(email)
                        }
                    }
                } else {
                    _signUpStatus.value = SignUpState.InputtingEmail
                }
            }
        }
    }

    fun onSignUpClick(phone: String) {
        // Email must be valid otherwise sign up button would not be displayed
        val email = requireNotNull(consumerEmail.value)
        viewModelScope.launch {
            // TODO(brnunes-stripe): Read formatted phone and country code from phone number element
            linkAccountManager.signUp(email, "+1$phone", "US").fold(
                onSuccess = {
                    onAccountFetched(it)
                },
                onFailure = ::onError
            )
        }
    }

    private suspend fun lookupConsumerEmail(email: String) {
        linkAccountManager.lookupConsumer(email).fold(
            onSuccess = {
                if (it != null) {
                    onAccountFetched(it)
                } else {
                    _signUpStatus.value = SignUpState.InputtingPhone
                }
            },
            onFailure = ::onError
        )
    }

    private fun onAccountFetched(linkAccount: LinkAccount) {
        navigator.navigateTo(
            if (linkAccount.isVerified) {
                LinkScreen.Wallet
            } else {
                LinkScreen.Verification
            }
        )
    }

    private fun onError(error: Throwable) {
        logger.error(error.localizedMessage ?: "Internal error.")
        // TODO(brnunes-stripe): Add localized error messages, show them in UI.
    }

    internal class Factory(
        private val starterArgs: LinkActivityContract.Args,
        private val injector: Injector
    ) : ViewModelProvider.Factory, Injectable<Unit> {

        @Inject
        lateinit var subComponentBuilderProvider:
            Provider<LinkViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            injector.inject(this)
            return subComponentBuilderProvider.get()
                .args(starterArgs)
                .build().signUpViewModel as T
        }

        /**
         * There's no need to implement fallback because the ViewModel receives the injector
         * directly as a parameter.
         */
        override fun fallbackInitialize(arg: Unit) {}
    }

    companion object {
        // How long to wait (in milliseconds) before triggering a call to lookup the email
        const val LOOKUP_DEBOUNCE_MS = 700L
    }
}
