package com.stripe.android.link.ui.inline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.Logger
import com.stripe.android.core.model.CountryCode
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.injection.CUSTOMER_EMAIL
import com.stripe.android.link.injection.CUSTOMER_PHONE
import com.stripe.android.link.injection.LINK_INTENT
import com.stripe.android.link.injection.MERCHANT_NAME
import com.stripe.android.link.ui.signup.SignUpState
import com.stripe.android.link.ui.signup.SignUpViewModel
import com.stripe.android.model.StripeIntent
import com.stripe.android.ui.core.elements.PhoneNumberController
import com.stripe.android.ui.core.elements.SimpleTextFieldController
import com.stripe.android.ui.core.injection.NonFallbackInjectable
import com.stripe.android.ui.core.injection.NonFallbackInjector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

internal class InlineSignupViewModel @Inject constructor(
    @Named(LINK_INTENT) val stripeIntent: StripeIntent,
    @Named(MERCHANT_NAME) val merchantName: String,
    @Named(CUSTOMER_EMAIL) customerEmail: String?,
    @Named(CUSTOMER_PHONE) customerPhone: String?,
    private val linkAccountManager: LinkAccountManager,
    private val linkEventsReporter: LinkEventsReporter,
    private val logger: Logger
) : ViewModel() {
    private val prefilledEmail =
        if (linkAccountManager.hasUserLoggedOut(customerEmail)) null else customerEmail
    private val prefilledPhone =
        customerPhone?.takeUnless { linkAccountManager.hasUserLoggedOut(customerEmail) } ?: ""

    val emailController = SimpleTextFieldController.createEmailSectionController(prefilledEmail)
    val phoneController = PhoneNumberController.createPhoneNumberController(prefilledPhone)
    val nameController = SimpleTextFieldController.createNameSectionController(null) // TODO

    val requiresNameCollection: Boolean
        get() = stripeIntent.countryCode != CountryCode.US.value

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

    private val consumerName: StateFlow<String?> =
        nameController.formFieldValue.map { it.takeIf { it.isComplete }?.value }
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
            linkEventsReporter.onInlineSignupCheckboxChecked()
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
                } else if (it == SignUpState.InputtingPhoneOrName) {
                    createUserInput(phoneNumber = consumerPhoneNumber.value, name = consumerName.value)
                }
            },
            onValidEmailEntered = {
                viewModelScope.launch {
                    lookupConsumerEmail(it)
                }
            }
        )

        viewModelScope.launch {
            combine(
                consumerPhoneNumber,
                consumerName,
                this@InlineSignupViewModel::createUserInput
            ).collect {
                userInput.value = it
            }
        }
    }

    private fun createUserInput(
        phoneNumber: String?,
        name: String?
    ): UserInput? {
        return if (phoneNumber != null) {
            // Email must be valid otherwise phone number and name collection UI would not be visible
            val email = requireNotNull(consumerEmail.value)
            val isNameValid = !requiresNameCollection || !name.isNullOrBlank()

            // TODO: Should the phone number be checked for validity?

            val phone = phoneController.getE164PhoneNumber(phoneNumber)
            val country = phoneController.getCountryCode()

            UserInput.SignUp(email, phone, country, name).takeIf { isNameValid }
        } else {
            null
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
                    _signUpStatus.value = SignUpState.InputtingPhoneOrName
                    linkEventsReporter.onSignupStarted(true)
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
