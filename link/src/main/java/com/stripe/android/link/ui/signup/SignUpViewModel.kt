package com.stripe.android.link.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.injection.NonFallbackInjectable
import com.stripe.android.link.injection.NonFallbackInjector
import com.stripe.android.link.injection.SignUpViewModelSubcomponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.Navigator
import com.stripe.android.ui.core.elements.SimpleTextFieldController
import com.stripe.android.ui.core.elements.TextFieldController
import kotlinx.coroutines.CoroutineScope
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
import javax.inject.Named
import javax.inject.Provider

/**
 * ViewModel that handles user sign up logic.
 */
internal class SignUpViewModel @Inject constructor(
    args: LinkActivityContract.Args,
    @Named(PREFILLED_EMAIL) private val customerEmail: String?,
    private val linkAccountManager: LinkAccountManager,
    private val navigator: Navigator,
    private val logger: Logger
) : ViewModel() {
    private val prefilledEmail =
        if (linkAccountManager.hasUserLoggedOut(customerEmail)) null else customerEmail

    val merchantName: String = args.merchantName
    val emailController: TextFieldController = SimpleTextFieldController
        .createEmailSectionController(prefilledEmail)

    /**
     * Emits the email entered in the form if valid, null otherwise.
     */
    private val consumerEmail: StateFlow<String?> =
        emailController.formFieldValue.map {
            // formFieldsList contains only one element, for the email. Take the second value of
            // the pair, which is the FormFieldEntry containing the value entered by the user.
            it.takeIf { it.isComplete }?.value
        }.stateIn(viewModelScope, SharingStarted.Lazily, prefilledEmail)

    private val _signUpStatus = MutableStateFlow(SignUpState.InputtingEmail)
    val signUpState: StateFlow<SignUpState> = _signUpStatus

    private val debouncer = Debouncer(prefilledEmail)

    init {
        debouncer.startWatching(
            coroutineScope = viewModelScope,
            emailFlow = consumerEmail,
            onStateChanged = {
                _signUpStatus.value = it
            },
            onValidEmailEntered = {
                viewModelScope.launch {
                    lookupConsumerEmail(it)
                }
            }
        )
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
        if (linkAccount.isVerified) {
            navigator.navigateTo(LinkScreen.Wallet, clearBackStack = true)
        } else {
            navigator.navigateTo(LinkScreen.Verification)
            // The sign up screen stays in the back stack.
            // Clean up the state in case the user comes back.
            emailController.onRawValueChange("")
        }
    }

    private fun onError(error: Throwable) {
        logger.error(error.localizedMessage ?: "Internal error.")
        // TODO(brnunes-stripe): Add localized error messages, show them in UI.
    }

    internal class Debouncer(
        private val initialEmail: String?
    ) {
        /**
         * Holds a Job that looks up the email after a delay, so that we can cancel it if the user
         * continues typing.
         */
        private var lookupJob: Job? = null

        fun startWatching(
            coroutineScope: CoroutineScope,
            emailFlow: StateFlow<String?>,
            onStateChanged: (SignUpState) -> Unit,
            onValidEmailEntered: (String) -> Unit
        ) {
            coroutineScope.launch {
                emailFlow.collect { email ->
                    // The first emitted value is the one provided in the constructor arguments, and
                    // shouldn't trigger a lookup.
                    if (email == initialEmail && lookupJob == null) {
                        // If it's a valid email, collect phone number
                        if (email != null) {
                            onStateChanged(SignUpState.InputtingPhone)
                        }
                        return@collect
                    }

                    lookupJob?.cancel()

                    if (email != null) {
                        lookupJob = launch {
                            delay(LOOKUP_DEBOUNCE_MS)
                            if (isActive) {
                                onStateChanged(SignUpState.VerifyingEmail)
                                onValidEmailEntered(email)
                            }
                        }
                    } else {
                        onStateChanged(SignUpState.InputtingEmail)
                    }
                }
            }
        }
    }

    internal class Factory(
        private val injector: NonFallbackInjector,
        private val email: String?
    ) : ViewModelProvider.Factory, NonFallbackInjectable {

        @Inject
        lateinit var subComponentBuilderProvider:
            Provider<SignUpViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            injector.inject(this)
            return subComponentBuilderProvider.get()
                .prefilledEmail(email)
                .build().signUpViewModel as T
        }
    }

    companion object {
        // How long to wait (in milliseconds) before triggering a call to lookup the email
        const val LOOKUP_DEBOUNCE_MS = 700L

        const val PREFILLED_EMAIL = "prefilled_email"
    }
}
