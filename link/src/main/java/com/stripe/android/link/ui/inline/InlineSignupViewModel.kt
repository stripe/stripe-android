package com.stripe.android.link.ui.inline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.model.CountryCode
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.getErrorMessage
import com.stripe.android.link.ui.signup.SignUpState
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.SectionController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class InlineSignupViewModel @Inject constructor(
    private val config: LinkConfiguration,
    private val linkAccountManager: LinkAccountManager,
    private val linkEventsReporter: LinkEventsReporter,
    private val logger: Logger,
) : ViewModel() {

    private val shouldPrefill = config.customerInfo.shouldPrefill
    private val prefilledEmail = config.customerInfo.email.takeIf { shouldPrefill }
    private val prefilledPhone = config.customerInfo.phone.takeIf { shouldPrefill }.orEmpty()
    private val prefilledName = config.customerInfo.name.takeIf { shouldPrefill }

    val emailController = EmailConfig.createController(
        initialValue = prefilledEmail,
        showOptionalLabel = config.showOptionalLabel,
    )

    val phoneController = PhoneNumberController.createPhoneNumberController(
        initialValue = prefilledPhone,
        initiallySelectedCountryCode = config.customerInfo.billingCountryCode,
    )

    val nameController = NameConfig.createController(prefilledName)

    val sectionController: SectionController = SectionController(
        label = null,
        sectionFieldErrorControllers = listOfNotNull(
            emailController,
            phoneController,
            nameController.takeIf { requiresNameCollection },
        ),
    )

    /**
     * Emits the email entered in the form if valid, null otherwise.
     */
    private val consumerEmail: StateFlow<String?> =
        emailController.formFieldValue.map {
            it.takeIf { it.isComplete }?.value
        }.stateIn(viewModelScope, SharingStarted.Lazily, prefilledEmail)

    /**
     * Emits the phone number entered in the form if valid, null otherwise.
     */
    private val consumerPhoneNumber: StateFlow<String?> =
        phoneController.formFieldValue.map { it.takeIf { it.isComplete }?.value }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    /**
     * Emits the name entered in the form if valid, null otherwise.
     */
    private val consumerName: StateFlow<String?> =
        nameController.formFieldValue.map { it.takeIf { it.isComplete }?.value }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _viewState =
        MutableStateFlow(
            InlineSignupViewState(
                userInput = null,
                merchantName = config.merchantName,
                signupMode = config.signupMode,
                isExpanded = false,
                apiFailed = false,
                signUpState = SignUpState.InputtingEmail
            )
        )
    val viewState: StateFlow<InlineSignupViewState> = _viewState

    private val _errorMessage = MutableStateFlow<ErrorMessage?>(null)
    val errorMessage: StateFlow<ErrorMessage?> = _errorMessage

    val requiresNameCollection: Boolean
        get() {
            val countryCode = when (val stripeIntent = config.stripeIntent) {
                is PaymentIntent -> stripeIntent.countryCode
                is SetupIntent -> stripeIntent.countryCode
            }
            return countryCode != CountryCode.US.value
        }

    private var hasExpanded = false

    private var debouncer = Debouncer()

    init {
        watchUserInput()
    }

    fun toggleExpanded() {
        _viewState.update { oldState ->
            oldState.copy(isExpanded = !oldState.isExpanded)
        }
        // First time user checks the box, start listening to inputs
        if (_viewState.value.isExpanded && !hasExpanded) {
            hasExpanded = true
            linkEventsReporter.onInlineSignupCheckboxChecked()
        }
    }

    private fun watchUserInput() {
        debouncer.startWatching(
            coroutineScope = viewModelScope,
            emailFlow = consumerEmail,
            onStateChanged = { signUpState ->
                clearError()
                _viewState.update { oldState ->
                    oldState.copy(
                        signUpState = signUpState,
                        userInput = when (signUpState) {
                            SignUpState.InputtingEmail, SignUpState.VerifyingEmail -> oldState.userInput
                            SignUpState.InputtingPhoneOrName ->
                                mapToUserInput(
                                    email = consumerEmail.value,
                                    phoneNumber = consumerPhoneNumber.value,
                                    name = consumerName.value
                                )
                        }
                    )
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
                consumerEmail,
                consumerPhoneNumber,
                consumerName,
                this@InlineSignupViewModel::mapToUserInput
            ).collect {
                _viewState.update { oldState ->
                    oldState.copy(userInput = it)
                }
            }
        }
    }

    private fun mapToUserInput(
        email: String?,
        phoneNumber: String?,
        name: String?
    ): UserInput? {
        return if (email != null && phoneNumber != null) {
            val isNameValid = !requiresNameCollection || !name.isNullOrBlank()

            val phone = phoneController.getE164PhoneNumber(phoneNumber)
            val country = phoneController.getCountryCode()

            UserInput.SignUp(email, phone, country, name).takeIf { isNameValid }
        } else {
            null
        }
    }

    private suspend fun lookupConsumerEmail(email: String) {
        clearError()
        linkAccountManager.lookupConsumer(email, startSession = false).fold(
            onSuccess = {
                if (it != null) {
                    _viewState.update { oldState ->
                        oldState.copy(
                            userInput = UserInput.SignIn(email),
                            signUpState = SignUpState.InputtingEmail,
                            apiFailed = false
                        )
                    }
                } else {
                    _viewState.update { oldState ->
                        oldState.copy(
                            signUpState = SignUpState.InputtingPhoneOrName,
                            apiFailed = false
                        )
                    }
                    linkEventsReporter.onSignupStarted(true)
                }
            },
            onFailure = {
                _viewState.update { oldState ->
                    oldState.copy(
                        signUpState = SignUpState.InputtingEmail,
                        apiFailed = it is APIConnectionException
                    )
                }
                if (!(it is APIConnectionException)) {
                    onError(it)
                }
            }
        )
    }

    private fun clearError() {
        _errorMessage.value = null
    }

    private fun onError(error: Throwable) = error.getErrorMessage().let {
        logger.error("Error: ", error)
        _errorMessage.value = it
    }

    internal class Factory(
        private val linkComponent: LinkComponent
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return linkComponent.inlineSignupViewModel as T
        }
    }
}
