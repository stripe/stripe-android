package com.stripe.android.link.ui.signup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.Logger
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkDismissalCoordinator
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.LinkAuthResult
import com.stripe.android.link.account.toLinkAuthResult
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.link.withDismissalDisabled
import com.stripe.android.model.EmailSource
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

internal class SignUpViewModel @Inject constructor(
    private val configuration: LinkConfiguration,
    private val linkEventsReporter: LinkEventsReporter,
    private val logger: Logger,
    private val linkAccountManager: LinkAccountManager,
    private val savedStateHandle: SavedStateHandle,
    private val dismissalCoordinator: LinkDismissalCoordinator,
    private val navigateAndClearStack: (LinkScreen) -> Unit,
    private val moveToWeb: (Throwable) -> Unit,
    private val linkLaunchMode: LinkLaunchMode,
    private val dismissWithResult: (LinkActivityResult) -> Unit
) : ViewModel() {
    private val useLinkConfigurationCustomerInfo =
        savedStateHandle.get<Boolean>(USE_LINK_CONFIGURATION_CUSTOMER_INFO) ?: true
    private val customerInfo = configuration.customerInfo.takeIf { useLinkConfigurationCustomerInfo }

    val emailController = EmailConfig.createController(
        initialValue = customerInfo?.email
    )
    val phoneNumberController = PhoneNumberController.createPhoneNumberController(
        initialValue = customerInfo?.phone.orEmpty(),
        initiallySelectedCountryCode = customerInfo?.billingCountryCode
    )
    val nameController = NameConfig.createController(
        initialValue = customerInfo?.name
    )

    private val _state = MutableStateFlow(SignUpScreenState.create(configuration, customerInfo))
    val state: StateFlow<SignUpScreenState> = _state.asStateFlow()

    private var emailHasChanged = false

    init {
        viewModelScope.launch {
            signUpEnabledListener()
        }
        viewModelScope.launch {
            emailListener()
        }
        linkEventsReporter.onSignupFlowPresented()
    }

    private suspend fun signUpEnabledListener() {
        combine(
            flow = nameController.fieldState.map {
                if (state.value.requiresNameCollection) {
                    it.isValid()
                } else {
                    true
                }
            },
            flow2 = emailController.fieldState.map { it.isValid() },
            flow3 = phoneNumberController.isComplete
        ) { nameComplete, emailComplete, phoneComplete ->
            nameComplete && emailComplete && phoneComplete
        }.collectLatest { formValid ->
            updateState { it.copy(signUpEnabled = formValid) }
        }
    }

    private suspend fun emailListener() {
        emailController.formFieldValue.mapLatest { entry ->
            entry.takeIf { it.isComplete }?.value
        }.collectLatest { email ->
            onError(null)
            delay(LOOKUP_DEBOUNCE)
            if (email != null) {
                if (email != configuration.customerInfo.email || emailHasChanged) {
                    lookupEmail(email)
                } else {
                    updateSignUpState(SignUpState.InputtingRemainingFields)
                }
            } else {
                updateSignUpState(SignUpState.InputtingPrimaryField)
            }

            if (email != configuration.customerInfo.email) {
                emailHasChanged = true
            }
        }
    }

    private suspend fun lookupEmail(email: String) {
        updateSignUpState(SignUpState.VerifyingEmail)

        val lookupResult = linkAccountManager.lookupByEmail(
            email = email,
            emailSource = EmailSource.USER_ACTION,
            startSession = true,
            customerId = configuration.customerIdForEceDefaultValues
        )

        updateSignUpState(SignUpState.InputtingPrimaryField)

        handleLookupResult(
            lookupResult = lookupResult,
            onNoLinkAccountFound = {
                // No Link account found -> display remaining fields for signup.
                updateSignUpState(SignUpState.InputtingRemainingFields)
                onError(null)
            }
        )
    }

    fun onSignUpClick() {
        clearError()
        viewModelScope.launch {
            updateState {
                it.copy(isSubmitting = true)
            }
            val email = emailController.fieldValue.value
            val lookupResult = dismissalCoordinator.withDismissalDisabled {
                linkAccountManager.lookupByEmail(
                    email = email,
                    emailSource = EmailSource.USER_ACTION,
                    startSession = true,
                    customerId = configuration.customerIdForEceDefaultValues
                )
            }
            handleLookupResult(
                lookupResult = lookupResult,
                onNoLinkAccountFound = { performSignup() }
            )
        }

        updateState {
            it.copy(isSubmitting = false)
        }
    }

    private suspend fun performSignup() {
        val signupResult = dismissalCoordinator.withDismissalDisabled {
            linkAccountManager.signUp(
                email = emailController.fieldValue.value,
                phoneNumber = phoneNumberController.getE164PhoneNumber(phoneNumberController.fieldValue.value),
                country = phoneNumberController.getCountryCode(),
                countryInferringMethod = "PHONE_NUMBER",
                name = nameController.fieldValue.value,
                consentAction = SignUpConsentAction.Implied
            )
        }

        when (val result = signupResult.toLinkAuthResult()) {
            is LinkAuthResult.Success -> {
                onAccountFetched(result.account)
                linkEventsReporter.onSignupCompleted()
            }
            is LinkAuthResult.NoLinkAccountFound -> {
                // This shouldn't happen during signup, but handle gracefully
                onError(RuntimeException("Unexpected no account found during signup"))
                linkEventsReporter.onSignupFailure(error = RuntimeException("No account found during signup"))
            }
            is LinkAuthResult.AttestationFailed -> {
                moveToWeb(result.error)
            }
            is LinkAuthResult.AccountError -> {
                handleAccountError(result.error)
            }
            is LinkAuthResult.Error -> {
                onError(result.error)
                linkEventsReporter.onSignupFailure(error = result.error)
            }
        }
    }

    // Extracted common result handling with custom handler for NoLinkAccountFound
    private suspend fun handleLookupResult(
        lookupResult: Result<LinkAccount?>,
        onNoLinkAccountFound: suspend () -> Unit
    ) {
        when (val result = lookupResult.toLinkAuthResult()) {
            is LinkAuthResult.Success -> {
                onAccountFetched(result.account)
                linkEventsReporter.onSignupCompleted()
            }
            is LinkAuthResult.NoLinkAccountFound -> {
                updateSignUpState(SignUpState.InputtingRemainingFields)
                onNoLinkAccountFound()
            }
            is LinkAuthResult.AttestationFailed -> {
                moveToWeb(result.error)
            }
            is LinkAuthResult.AccountError -> {
                handleAccountError(result.error)
            }
            is LinkAuthResult.Error -> {
                updateSignUpState(SignUpState.InputtingRemainingFields)
                onError(result.error)
            }
        }
    }

    private suspend fun handleAccountError(error: Throwable) {
        // Handle account error - logic from the original LinkAuthResult.AccountError.handle()
        updateSignUpState(SignUpState.InputtingPrimaryField)
        onError(
            error = error,
            errorMessage = R.string.stripe_signup_deactivated_account_message.resolvableString
        )
        linkEventsReporter.onSignupFailure(error = error)
    }

    private fun onAccountFetched(linkAccount: LinkAccount?) {
        val targetScreen = when {
            linkAccount?.completedSignup == true -> LinkScreen.PaymentMethod
            linkAccount?.isVerified == true -> LinkScreen.Wallet
            else -> LinkScreen.Verification
        }

        // Return the account in authentication mode if verification not required
        if (linkLaunchMode is LinkLaunchMode.Authentication && targetScreen != LinkScreen.Verification) {
            dismissWithResult(
                LinkActivityResult.Completed(
                    linkAccountUpdate = LinkAccountUpdate.Value(linkAccount),
                    selectedPayment = null,
                )
            )
        } else {
            navigateAndClearStack(targetScreen)
        }
    }

    private fun onError(
        error: Throwable?,
        errorMessage: ResolvableString? = error?.stripeErrorMessage()
    ) {
        if (error != null) {
            logger.error("SignUpViewModel Error: ", error)
        }
        updateState {
            it.copy(
                errorMessage = errorMessage
            )
        }
    }

    private fun clearError() {
        updateState { it.copy(errorMessage = null) }
    }

    private fun updateState(produceValue: (SignUpScreenState) -> SignUpScreenState) {
        _state.update(produceValue)
    }

    private fun updateSignUpState(signUpState: SignUpState) {
        updateState { old ->
            old.copy(signUpState = signUpState)
        }
    }

    companion object {
        // How long to wait before triggering a call to lookup the email
        internal val LOOKUP_DEBOUNCE = 1.seconds
        internal const val USE_LINK_CONFIGURATION_CUSTOMER_INFO = "use_link_configuration_customer_info"

        fun factory(
            parentComponent: NativeLinkComponent,
            navigateAndClearStack: (LinkScreen) -> Unit,
            moveToWeb: (Throwable) -> Unit,
            dismissWithResult: (LinkActivityResult) -> Unit
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    SignUpViewModel(
                        configuration = parentComponent.configuration,
                        linkEventsReporter = parentComponent.linkEventsReporter,
                        logger = parentComponent.logger,
                        linkAccountManager = parentComponent.linkAccountManager,
                        savedStateHandle = parentComponent.savedStateHandle,
                        dismissalCoordinator = parentComponent.dismissalCoordinator,
                        navigateAndClearStack = navigateAndClearStack,
                        moveToWeb = moveToWeb,
                        linkLaunchMode = parentComponent.linkLaunchMode,
                        dismissWithResult = dismissWithResult
                    )
                }
            }
        }
    }
}
