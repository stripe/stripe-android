package com.stripe.android.link.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.Logger
import com.stripe.android.core.model.CountryCode
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.injection.SignUpViewModelSubcomponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.Navigator
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.getErrorMessage
import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.ui.core.elements.PhoneNumberController
import com.stripe.android.ui.core.elements.SimpleTextFieldController
import com.stripe.android.ui.core.injection.NonFallbackInjectable
import com.stripe.android.ui.core.injection.NonFallbackInjector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    private val args: LinkActivityContract.Args,
    @Named(PREFILLED_EMAIL) private val customerEmail: String?,
    private val linkAccountManager: LinkAccountManager,
    private val linkEventsReporter: LinkEventsReporter,
    private val navigator: Navigator,
    private val logger: Logger
) : ViewModel() {
    private val prefilledEmail =
        if (linkAccountManager.hasUserLoggedOut(customerEmail)) null else customerEmail
    private val prefilledPhone =
        args.customerPhone?.takeUnless { linkAccountManager.hasUserLoggedOut(customerEmail) } ?: ""
    private val prefilledName =
        args.customerName?.takeUnless { linkAccountManager.hasUserLoggedOut(customerEmail) } ?: ""

    val merchantName: String = args.merchantName

    val emailController = SimpleTextFieldController.createEmailSectionController(prefilledEmail)
    val phoneController = PhoneNumberController.createPhoneNumberController(prefilledPhone)
    val nameController = SimpleTextFieldController.createNameSectionController(prefilledName)

    /**
     * Emits the email entered in the form if valid, null otherwise.
     */
    private val consumerEmail: StateFlow<String?> =
        emailController.formFieldValue.map { it.takeIf { it.isComplete }?.value }
            .stateIn(viewModelScope, SharingStarted.Eagerly, prefilledEmail)

    /**
     * Emits the phone number entered in the form if valid, null otherwise.
     */
    private val consumerPhoneNumber: StateFlow<String?> =
        phoneController.formFieldValue.map { it.takeIf { it.isComplete }?.value }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Emits the name entered in the form if valid, null otherwise.
     */
    private val consumerName: StateFlow<String?> =
        nameController.formFieldValue.map { it.takeIf { it.isComplete }?.value }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val requiresNameCollection: Boolean
        get() {
            val countryCode = when (val stripeIntent = args.stripeIntent) {
                is PaymentIntent -> stripeIntent.countryCode
                is SetupIntent -> stripeIntent.countryCode
            }
            return countryCode != CountryCode.US.value
        }

    private val _isReadyToSignUp = MutableStateFlow(false)
    val isReadyToSignUp: StateFlow<Boolean> = _isReadyToSignUp

    private val _signUpStatus = MutableStateFlow(SignUpState.InputtingEmail)
    val signUpState: StateFlow<SignUpState> = _signUpStatus

    private val _errorMessage = MutableStateFlow<ErrorMessage?>(null)
    val errorMessage: StateFlow<ErrorMessage?> = _errorMessage

    private val debouncer = Debouncer(prefilledEmail)

    init {
        debouncer.startWatching(
            coroutineScope = viewModelScope,
            emailFlow = consumerEmail,
            onStateChanged = {
                clearError()
                _signUpStatus.value = it
            },
            onValidEmailEntered = {
                viewModelScope.launch {
                    lookupConsumerEmail(it)
                }
            }
        )

        viewModelScope.launch {
            combine(
                consumerEmail,
                consumerPhoneNumber,
                consumerName,
                this@SignUpViewModel::determineIsReadyToSignUp
            ).collect {
                _isReadyToSignUp.value = it
            }
        }

        linkEventsReporter.onSignupFlowPresented()
    }

    private fun determineIsReadyToSignUp(
        email: String?,
        phone: String?,
        name: String?
    ): Boolean {
        return email != null && phone != null && (!requiresNameCollection || !name.isNullOrBlank())
    }

    fun onSignUpClick() {
        clearError()
        // All inputs must be valid otherwise sign up button would not be displayed
        val email = requireNotNull(consumerEmail.value)
        val phone = phoneController.getE164PhoneNumber(requireNotNull(consumerPhoneNumber.value))
        val country = phoneController.getCountryCode()
        val name = consumerName.value
        viewModelScope.launch {
            linkAccountManager.signUp(
                email,
                phone,
                country,
                name,
                ConsumerSignUpConsentAction.Button
            ).fold(
                onSuccess = {
                    onAccountFetched(it)
                    linkEventsReporter.onSignupCompleted()
                },
                onFailure = {
                    onError(it)
                    linkEventsReporter.onSignupFailure()
                }
            )
        }
    }

    private suspend fun lookupConsumerEmail(email: String) {
        clearError()
        linkAccountManager.lookupConsumer(email).fold(
            onSuccess = {
                if (it != null) {
                    onAccountFetched(it)
                } else {
                    _signUpStatus.value = SignUpState.InputtingPhoneOrName
                    linkEventsReporter.onSignupStarted()
                }
            },
            onFailure = {
                _signUpStatus.value = SignUpState.InputtingEmail
                onError(it)
            }
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

    private fun clearError() {
        _errorMessage.value = null
    }

    private fun onError(error: Throwable) = error.getErrorMessage().let {
        logger.error("Error: ", error)
        _errorMessage.value = it
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
                            onStateChanged(SignUpState.InputtingPhoneOrName)
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
        const val LOOKUP_DEBOUNCE_MS = 1000L

        const val PREFILLED_EMAIL = "prefilled_email"
    }
}
