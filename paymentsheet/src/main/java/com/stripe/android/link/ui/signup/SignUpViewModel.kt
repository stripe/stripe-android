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
import com.stripe.android.link.LinkLaunchMode.Authentication
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.NoLinkAccountFoundException
import com.stripe.android.link.account.LinkAuth
import com.stripe.android.link.account.LinkAuthResult
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
    private val linkAuth: LinkAuth,
    private val savedStateHandle: SavedStateHandle,
    private val dismissalCoordinator: LinkDismissalCoordinator,
    private val navigateAndClearStack: (LinkScreen) -> Unit,
    private val moveToWeb: () -> Unit,
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

        val lookupResult = linkAuth.lookUp(
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
                linkAuth.lookUp(
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
            linkAuth.signUp(
                email = emailController.fieldValue.value,
                phoneNumber = phoneNumberController.getE164PhoneNumber(phoneNumberController.fieldValue.value),
                country = phoneNumberController.getCountryCode(),
                name = nameController.fieldValue.value,
                consentAction = SignUpConsentAction.Implied
            )
        }

        when (signupResult) {
            is LinkAuthResult.AttestationFailed -> {
                moveToWeb()
            }
            is LinkAuthResult.Error -> {
                onError(signupResult.error)
                linkEventsReporter.onSignupFailure(error = signupResult.error)
            }
            is LinkAuthResult.Success -> {
                onAccountFetched(signupResult.account)
                linkEventsReporter.onSignupCompleted()
            }
            LinkAuthResult.NoLinkAccountFound -> {
                onError(NoLinkAccountFoundException())
                linkEventsReporter.onSignupFailure(error = NoLinkAccountFoundException())
            }
            is LinkAuthResult.AccountError -> {
                signupResult.handle()
            }
        }
    }

    // Extracted common result handling with custom handler for NoLinkAccountFound
    private suspend fun handleLookupResult(
        lookupResult: LinkAuthResult,
        onNoLinkAccountFound: suspend () -> Unit
    ) {
        when (lookupResult) {
            is LinkAuthResult.AttestationFailed -> {
                moveToWeb()
            }
            is LinkAuthResult.Error -> {
                updateSignUpState(SignUpState.InputtingRemainingFields)
                onError(lookupResult.error)
            }
            is LinkAuthResult.Success -> {
                onAccountFetched(lookupResult.account)
                linkEventsReporter.onSignupCompleted()
            }
            LinkAuthResult.NoLinkAccountFound -> {
                onNoLinkAccountFound()
            }
            is LinkAuthResult.AccountError -> {
                lookupResult.handle()
            }
        }
    }

    private fun onAccountFetched(linkAccount: LinkAccount?) {
        val targetScreen = when {
            linkAccount?.completedSignup == true -> LinkScreen.PaymentMethod
            linkAccount?.isVerified == true -> LinkScreen.Wallet
            else -> LinkScreen.Verification
        }

        // Return the account in authentication mode if verification not required
        if (linkLaunchMode is Authentication && targetScreen != LinkScreen.Verification) {
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

    private fun LinkAuthResult.AccountError.handle() {
        updateSignUpState(SignUpState.InputtingPrimaryField)
        onError(
            error = error,
            errorMessage = R.string.stripe_signup_deactivated_account_message.resolvableString
        )
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
            moveToWeb: () -> Unit,
            dismissWithResult: (LinkActivityResult) -> Unit
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    SignUpViewModel(
                        configuration = parentComponent.configuration,
                        linkEventsReporter = parentComponent.linkEventsReporter,
                        logger = parentComponent.logger,
                        linkAuth = parentComponent.linkAuth,
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
