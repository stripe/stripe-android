package com.stripe.android.link.ui.inline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.Logger
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.injection.CUSTOMER_EMAIL
import com.stripe.android.link.injection.MERCHANT_NAME
import com.stripe.android.link.injection.NonFallbackInjectable
import com.stripe.android.link.injection.NonFallbackInjector
import com.stripe.android.link.ui.signup.SignUpState
import com.stripe.android.link.ui.signup.SignUpViewModel
import com.stripe.android.ui.core.elements.PhoneNumberController
import com.stripe.android.ui.core.elements.SimpleTextFieldController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

internal class InlineSignupViewModel @Inject constructor(
    @Named(MERCHANT_NAME) val merchantName: String,
    @Named(CUSTOMER_EMAIL) customerEmail: String?,
    private val linkAccountManager: LinkAccountManager,
    private val logger: Logger
) : ViewModel() {
    private val prefilledEmail =
        if (linkAccountManager.hasUserLoggedOut(customerEmail)) null else customerEmail

    val emailController = SimpleTextFieldController.createEmailSectionController(
        prefilledEmail
    )

    val phoneController: PhoneNumberController =
        PhoneNumberController.createPhoneNumberController()

    /**
     * Emits the email entered in the form if valid, null otherwise.
     */
    private val consumerEmail: StateFlow<String?> =
        emailController.formFieldValue.map { it.takeIf { it.isComplete }?.value }
            .stateIn(viewModelScope, SharingStarted.Lazily, prefilledEmail)

    /**
     * Emits the phone number entered in the form if valid, null otherwise.
     */
    private val consumerPhoneNumber: StateFlow<String?> =
        phoneController.formFieldValue.map { it.takeIf { it.isComplete }?.value }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _signUpStatus = MutableStateFlow(SignUpState.InputtingEmail)
    val signUpState: StateFlow<SignUpState> = _signUpStatus

    val isExpanded = MutableStateFlow(false)

    /**
     * The collected input from the user, always valid unless null.
     * When not null, enough information has been collected to proceed with the payment flow.
     * This means that the user has entered an email that already has a link account and just
     * needs verification, or entered a new email and phone number.
     */
    val userInput = MutableStateFlow<UserInput?>(null)
    private var hasExpanded = false

    private var debouncer = SignUpViewModel.Debouncer(prefilledEmail)

    fun toggleExpanded() {
        isExpanded.value = !isExpanded.value
        // First time user checks the box, start listening to inputs
        if (isExpanded.value && !hasExpanded) {
            hasExpanded = true
            watchUserInput()
        }
    }

    private fun watchUserInput() {
        debouncer.startWatching(
            coroutineScope = viewModelScope,
            emailFlow = consumerEmail,
            onStateChanged = {
                _signUpStatus.value = it
                if (it == SignUpState.InputtingEmail || it == SignUpState.VerifyingEmail) {
                    userInput.value = null
                } else if (it == SignUpState.InputtingPhone) {
                    onPhoneInput(consumerPhoneNumber.value)
                }
            },
            onValidEmailEntered = {
                viewModelScope.launch {
                    lookupConsumerEmail(it)
                }
            }
        )

        viewModelScope.launch {
            consumerPhoneNumber.collect {
                onPhoneInput(it)
            }
        }
    }

    private fun onPhoneInput(phoneNumber: String?) {
        if (phoneNumber != null) {
            // Email must be valid otherwise phone number collection UI would not be visible
            val email = requireNotNull(consumerEmail.value)
            val phone = phoneController.getE164PhoneNumber(phoneNumber)
            val country = phoneController.getCountryCode()

            userInput.value = UserInput.SignUp(email, phone, country)
        } else {
            userInput.value = null
        }
    }

    private suspend fun lookupConsumerEmail(email: String) {
        linkAccountManager.lookupConsumer(email, startSession = false).fold(
            onSuccess = {
                if (it != null) {
                    userInput.value = UserInput.SignIn(email)
                    _signUpStatus.value = SignUpState.InputtingEmail
                } else {
                    userInput.value = null
                    _signUpStatus.value = SignUpState.InputtingPhone
                }
            },
            onFailure = ::onError
        )
    }

    private fun onError(error: Throwable) {
        logger.error(error.localizedMessage ?: "Internal error.")
        // TODO(brnunes-stripe): Add localized error messages, show them in UI.
    }

    internal class Factory(
        private val injector: NonFallbackInjector
    ) : ViewModelProvider.Factory, NonFallbackInjectable {

        @Inject
        lateinit var viewModel: InlineSignupViewModel

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            injector.inject(this)
            return viewModel as T
        }
    }
}
