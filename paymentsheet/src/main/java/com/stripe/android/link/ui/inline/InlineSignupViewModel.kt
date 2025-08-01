package com.stripe.android.link.ui.inline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.injection.LinkComponent
import com.stripe.android.link.ui.inline.LinkSignupField.Email
import com.stripe.android.link.ui.inline.LinkSignupField.Name
import com.stripe.android.link.ui.inline.LinkSignupField.Phone
import com.stripe.android.link.ui.signup.SignUpState
import com.stripe.android.link.utils.errorMessage
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val LOOKUP_DEBOUNCE_MS = 1000L

internal class InlineSignupViewModel(
    val initialUserInput: UserInput?,
    val signupMode: LinkSignupMode,
    private val config: LinkConfiguration,
    private val linkAccountManager: LinkAccountManager,
    private val linkEventsReporter: LinkEventsReporter,
    private val logger: Logger,
    private val lookupDelay: Long = LOOKUP_DEBOUNCE_MS,
) : ViewModel() {
    @AssistedInject
    constructor(
        @Assisted initialUserInput: UserInput?,
        @Assisted signupMode: LinkSignupMode,
        config: LinkConfiguration,
        linkAccountManager: LinkAccountManager,
        linkEventsReporter: LinkEventsReporter,
        logger: Logger,
    ) : this(
        initialUserInput = initialUserInput,
        signupMode = signupMode,
        config = config,
        linkAccountManager = linkAccountManager,
        linkEventsReporter = linkEventsReporter,
        logger = logger,
        lookupDelay = LOOKUP_DEBOUNCE_MS,
    )

    private val hasInitialUserInput = initialUserInput != null

    private val initialEmail = initialUserInput?.email()
    private val initialPhone = initialUserInput?.phone()
    private val initialName = initialUserInput?.name()
    private val initialCountry = initialUserInput?.country()

    private val initialViewState = InlineSignupViewState.create(
        signupMode = signupMode,
        config = config,
        initialEmail = initialEmail,
        initialPhone = initialPhone,
        isExpanded = if (config.linkSignUpOptInFeatureEnabled) {
            config.linkSignUpOptInInitialValue
        } else {
            hasInitialUserInput
        },
    )
    private val _viewState = MutableStateFlow(initialViewState)
    val viewState: StateFlow<InlineSignupViewState> = _viewState

    private val showOptionalLabel = signupMode == LinkSignupMode.AlongsideSaveForFutureUse
    private val prefillEligibleFields = initialViewState.prefillEligibleFields

    private val prefilledEmail = config.customerInfo.email.takeIf { Email in prefillEligibleFields }
    private val prefilledPhone = config.customerInfo.phone.takeIf { Phone in prefillEligibleFields }.orEmpty()
    private val prefilledName = config.customerInfo.name.takeIf { Name in prefillEligibleFields }

    val emailController = EmailConfig.createController(
        initialValue = initialEmail ?: prefilledEmail,
        showOptionalLabel = initialViewState.isShowingEmailFirst && showOptionalLabel,
    )

    val phoneController = PhoneNumberController.createPhoneNumberController(
        initialValue = initialPhone ?: prefilledPhone,
        initiallySelectedCountryCode = initialCountry ?: config.customerInfo.billingCountryCode,
        showOptionalLabel = initialViewState.isShowingPhoneFirst && showOptionalLabel,
    )

    val nameController = NameConfig.createController(initialValue = initialName ?: prefilledName)

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

    private val _errorMessage = MutableStateFlow<ResolvableString?>(null)
    val errorMessage: StateFlow<ResolvableString?> = _errorMessage.asStateFlow()

    val requiresNameCollection: Boolean
        get() = Name in initialViewState.fields

    private var hasExpanded = if (config.linkSignUpOptInFeatureEnabled) {
        config.linkSignUpOptInInitialValue
    } else {
        hasInitialUserInput
    }

    init {
        watchUserInput()
    }

    fun toggleExpanded() {
        _viewState.update { oldState ->
            oldState.copy(
                isExpanded = !oldState.isExpanded,
                userHasInteracted = true
            )
        }

        // First time user checks the box, start listening to inputs
        if (_viewState.value.isExpanded && !hasExpanded) {
            hasExpanded = true
            linkEventsReporter.onInlineSignupCheckboxChecked()
        }
    }

    fun changeSignupDetails() {
        _viewState.update {
            it.copy(didAskToChangeSignupDetails = true)
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
                _viewState.mapAsStateFlow { it.userHasInteracted },
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
                                name = consumerName.value,
                                userHasInteracted = oldState.userHasInteracted
                            )
                    }
                )
            }
        }

        val itemsToSkip = if (dropFirst) 1 else 0

        consumerEmail.drop(itemsToSkip).collectLatest { email ->
            if (!email.isNullOrBlank()) {
                delay(lookupDelay)
                onStateChanged(SignUpState.VerifyingEmail)
                lookupConsumerEmail(email)
            } else {
                onStateChanged(initialViewState.signUpState)
            }
        }
    }

    private fun mapToUserInput(
        email: String?,
        phoneNumber: String?,
        name: String?,
        userHasInteracted: Boolean
    ): UserInput? {
        val signUpMode = initialViewState.signupMode
        val meetsPhoneNumberCriteria = initialViewState.linkSignUpOptInFeatureEnabled ||
            phoneNumber != null
        return if (email != null && meetsPhoneNumberCriteria && signUpMode != null) {
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
                    defaultOptIn = initialViewState.allowsDefaultOptIn,
                    linkSignUpOptInFeatureEnabled = initialViewState.linkSignUpOptInFeatureEnabled,
                    linkSignUpInitialValue = config.linkSignUpOptInInitialValue,
                    userHasInteracted = userHasInteracted
                )
            ).takeIf { isNameValid }
        } else {
            null
        }
    }

    private suspend fun lookupConsumerEmail(email: String) {
        clearError()
        linkAccountManager.lookupConsumer(
            email = email,
            startSession = false,
            customerId = null
        ).fold(
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

    private fun UserInput.email(): String {
        return when (this) {
            is UserInput.SignUp -> email
            is UserInput.SignIn -> email
        }
    }

    private fun UserInput.phone(): String? {
        return when (this) {
            is UserInput.SignUp -> phone
            is UserInput.SignIn -> null
        }
    }

    private fun UserInput.name(): String? {
        return when (this) {
            is UserInput.SignUp -> name
            is UserInput.SignIn -> null
        }
    }

    private fun UserInput.country(): String? {
        return when (this) {
            is UserInput.SignUp -> country
            is UserInput.SignIn -> null
        }
    }

    private fun clearError() {
        _errorMessage.value = null
    }

    private fun onError(error: Throwable) = error.errorMessage.let {
        logger.error("Error: ", error)
        _errorMessage.value = it
    }

    private fun LinkSignupMode.toConsentAction(
        hasPrefilledEmail: Boolean,
        hasPrefilledPhone: Boolean,
        defaultOptIn: Boolean,
        linkSignUpOptInFeatureEnabled: Boolean,
        userHasInteracted: Boolean,
        linkSignUpInitialValue: Boolean
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
                    linkSignUpOptInFeatureEnabled -> {
                        if (linkSignUpInitialValue && !userHasInteracted) {
                            SignUpConsentAction.SignUpOptInMobilePrechecked
                        } else {
                            SignUpConsentAction.SignUpOptInMobileChecked
                        }
                    }
                    defaultOptIn -> getDefaultOptInConsentAction(hasPrefilledEmail, hasPrefilledPhone)
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

    private fun getDefaultOptInConsentAction(
        hasPrefilledEmail: Boolean,
        hasPrefilledPhone: Boolean
    ): SignUpConsentAction = if (hasPrefilledEmail && hasPrefilledPhone) {
        SignUpConsentAction.DefaultOptInWithAllPrefilled
    } else if (hasPrefilledEmail || hasPrefilledPhone) {
        SignUpConsentAction.DefaultOptInWithSomePrefilled
    } else {
        SignUpConsentAction.DefaultOptInWithNonePrefilled
    }

    internal class Factory(
        private val signupMode: LinkSignupMode,
        private val initialUserInput: UserInput?,
        private val linkComponent: LinkComponent
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return linkComponent.inlineSignupViewModelFactory.create(signupMode, initialUserInput) as T
        }
    }
}
