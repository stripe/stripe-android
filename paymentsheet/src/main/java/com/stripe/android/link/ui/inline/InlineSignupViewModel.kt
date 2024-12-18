package com.stripe.android.link.ui.inline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.getErrorMessage
import com.stripe.android.link.ui.inline.LinkSignupField.Email
import com.stripe.android.link.ui.inline.LinkSignupField.Name
import com.stripe.android.link.ui.inline.LinkSignupField.Phone
import com.stripe.android.link.ui.signup.SignUpState
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberController
import com.stripe.android.uicore.elements.SectionController
import com.stripe.android.uicore.utils.mapAsStateFlow
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val LOOKUP_DEBOUNCE_MS = 1000L

internal class InlineSignupViewModel @AssistedInject constructor(
    @Assisted val signupMode: LinkSignupMode,
    config: LinkConfiguration,
    private val linkAccountManager: LinkAccountManager,
    private val linkEventsReporter: LinkEventsReporter,
    private val logger: Logger,
) : ViewModel() {

    private val initialViewState = InlineSignupViewState.create(signupMode, config)
    private val _viewState = MutableStateFlow(initialViewState)
    val viewState: StateFlow<InlineSignupViewState> = _viewState

    private val showOptionalLabel = signupMode == LinkSignupMode.AlongsideSaveForFutureUse
    private val prefillEligibleFields = initialViewState.prefillEligibleFields

    private val prefilledEmail = config.customerInfo.email.takeIf { Email in prefillEligibleFields }
    private val prefilledPhone = config.customerInfo.phone.takeIf { Phone in prefillEligibleFields }.orEmpty()
    private val prefilledName = config.customerInfo.name.takeIf { Name in prefillEligibleFields }

    val emailController = EmailConfig.createController(
        initialValue = prefilledEmail,
        showOptionalLabel = initialViewState.isShowingEmailFirst && showOptionalLabel,
    )

    val phoneController = PhoneNumberController.createPhoneNumberController(
        initialValue = prefilledPhone,
        initiallySelectedCountryCode = config.customerInfo.billingCountryCode,
        showOptionalLabel = initialViewState.isShowingPhoneFirst && showOptionalLabel,
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
        emailController.formFieldValue.mapAsStateFlow {
            it.takeIf { it.isComplete }?.value
        }

    /**
     * Emits the phone number entered in the form if valid, null otherwise.
     */
    private val consumerPhoneNumber: StateFlow<String?> =
        phoneController.formFieldValue.mapAsStateFlow { it.takeIf { it.isComplete }?.value }

    /**
     * Emits the name entered in the form if valid, null otherwise.
     */
    private val consumerName: StateFlow<String?> =
        nameController.formFieldValue.mapAsStateFlow { it.takeIf { it.isComplete }?.value }

    private val _errorMessage = MutableStateFlow<ErrorMessage?>(null)
    val errorMessage: StateFlow<ErrorMessage?> = _errorMessage

    val requiresNameCollection: Boolean
        get() = Name in initialViewState.fields

    private var hasExpanded = false

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
        viewModelScope.launch {
            if (initialViewState.isShowingPhoneFirst) {
                watchPhoneInput()
                watchEmailInput(dropFirst = true)
            } else {
                watchEmailInput()
            }
        }

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

    private suspend fun watchPhoneInput() {
        consumerPhoneNumber.first { !it.isNullOrBlank() }
        _viewState.update {
            it.copy(signUpState = SignUpState.InputtingRemainingFields)
        }
    }

    private suspend fun watchEmailInput(dropFirst: Boolean = false) {
        val onStateChanged: (SignUpState) -> Unit = { signUpState ->
            clearError()
            _viewState.update { oldState ->
                oldState.copy(
                    signUpState = signUpState,
                    userInput = when (signUpState) {
                        SignUpState.InputtingPrimaryField, SignUpState.VerifyingEmail -> oldState.userInput
                        SignUpState.InputtingRemainingFields ->
                            mapToUserInput(
                                email = consumerEmail.value,
                                phoneNumber = consumerPhoneNumber.value,
                                name = consumerName.value
                            )
                    }
                )
            }
        }

        val itemsToSkip = if (dropFirst) 1 else 0

        consumerEmail.drop(itemsToSkip).collectLatest { email ->
            if (!email.isNullOrBlank()) {
                delay(LOOKUP_DEBOUNCE_MS)
                onStateChanged(SignUpState.VerifyingEmail)
                lookupConsumerEmail(email)
            } else {
                onStateChanged(SignUpState.InputtingPrimaryField)
            }
        }
    }

    private fun mapToUserInput(
        email: String?,
        phoneNumber: String?,
        name: String?
    ): UserInput? {
        val signUpMode = initialViewState.signupMode

        return if (email != null && phoneNumber != null && signUpMode != null) {
            val isNameValid = !requiresNameCollection || !name.isNullOrBlank()
            val country = phoneController.getCountryCode()

            UserInput.SignUp(
                email = email,
                phone = phoneNumber,
                country = country,
                name = name,
                consentAction = signUpMode.toConsentAction(
                    hasPrefilledEmail = prefilledEmail != null,
                    hasPrefilledPhone = prefilledPhone.isNotBlank(),
                )
            ).takeIf { isNameValid }
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
                            signUpState = SignUpState.InputtingPrimaryField,
                            apiFailed = false
                        )
                    }
                } else {
                    _viewState.update { oldState ->
                        oldState.copy(
                            signUpState = SignUpState.InputtingRemainingFields,
                            apiFailed = false
                        )
                    }
                    linkEventsReporter.onSignupStarted(true)
                }
            },
            onFailure = {
                _viewState.update { oldState ->
                    oldState.copy(
                        signUpState = SignUpState.InputtingPrimaryField,
                        apiFailed = it is APIConnectionException
                    )
                }
                if (it !is APIConnectionException) {
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

    private fun LinkSignupMode.toConsentAction(
        hasPrefilledEmail: Boolean,
        hasPrefilledPhone: Boolean
    ): SignUpConsentAction {
        return when (this) {
            LinkSignupMode.AlongsideSaveForFutureUse -> {
                when (hasPrefilledEmail) {
                    true -> SignUpConsentAction.ImpliedWithPrefilledEmail
                    false -> SignUpConsentAction.Implied
                }
            }
            LinkSignupMode.InsteadOfSaveForFutureUse -> {
                when {
                    hasPrefilledEmail && hasPrefilledPhone ->
                        SignUpConsentAction.CheckboxWithPrefilledEmailAndPhone
                    hasPrefilledEmail ->
                        SignUpConsentAction.CheckboxWithPrefilledEmail
                    else ->
                        SignUpConsentAction.Checkbox
                }
            }
        }
    }

    internal class Factory(
        private val signupMode: LinkSignupMode,
        private val linkComponent: LinkComponent
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return linkComponent.inlineSignupViewModelFactory.create(signupMode) as T
        }
    }
}
