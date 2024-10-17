package com.stripe.android.link.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import com.stripe.android.core.Logger
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.analytics.LinkEventsReporter
import com.stripe.android.link.injection.NativeLinkComponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.ui.ErrorMessage
import com.stripe.android.link.ui.getErrorMessage
import com.stripe.android.link.ui.inline.SignUpConsentAction
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.uicore.elements.EmailConfig
import com.stripe.android.uicore.elements.NameConfig
import com.stripe.android.uicore.elements.PhoneNumberController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val linkAccountManager: LinkAccountManager,
    private val linkEventsReporter: LinkEventsReporter,
    private val logger: Logger
) : ViewModel() {
    internal var navController: NavHostController? = null
    val emailController = EmailConfig.createController(
        initialValue = configuration.customerInfo.email
    )
    val phoneNumberController = PhoneNumberController.createPhoneNumberController(
        initialValue = configuration.customerInfo.phone.orEmpty(),
        initiallySelectedCountryCode = configuration.customerInfo.billingCountryCode
    )
    val nameController = NameConfig.createController(
        initialValue = configuration.customerInfo.name
    )
    private val _state = MutableStateFlow(
        value = SignUpScreenState(
            signUpEnabled = false
        )
    )

    val state: StateFlow<SignUpScreenState> = _state

    private val requiresNameCollection: Boolean
        get() {
            val countryCode = when (val stripeIntent = configuration.stripeIntent) {
                is PaymentIntent -> stripeIntent.countryCode
                is SetupIntent -> stripeIntent.countryCode
            }
            return countryCode != CountryCode.US.value
        }

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
                if (requiresNameCollection) {
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
            delay(LOOKUP_DEBOUNCE)
            if (email != null) {
                updateSignUpState(SignUpState.VerifyingEmail)
                lookupConsumerEmail(email)
            } else {
                updateSignUpState(SignUpState.InputtingPrimaryField)
            }
        }
    }

    fun onSignUpClick() {
        clearError()
        viewModelScope.launch {
            linkAccountManager.signUp(
                email = emailController.fieldValue.value,
                phone = phoneNumberController.fieldValue.value,
                country = phoneNumberController.getCountryCode(),
                name = nameController.fieldValue.value,
                consentAction = SignUpConsentAction.Implied
            ).fold(
                onSuccess = {
                    onAccountFetched(it)
                    linkEventsReporter.onSignupCompleted()
                },
                onFailure = {
                    onError(it)
                    linkEventsReporter.onSignupFailure(error = it)
                }
            )
        }
    }

    private fun onAccountFetched(linkAccount: LinkAccount?) {
        if (linkAccount?.isVerified == true) {
            navController?.popBackStack(LinkScreen.Wallet.route, inclusive = false)
        } else {
            navController?.navigate(LinkScreen.Verification.route)
            // The sign up screen stays in the back stack.
            // Clean up the state in case the user comes back.
            emailController.onValueChange("")
        }
    }

    private suspend fun lookupConsumerEmail(email: String) {
        clearError()
        linkAccountManager.lookupConsumer(email).fold(
            onSuccess = {
                if (it != null) {
                    onAccountFetched(it)
                } else {
                    updateSignUpState(SignUpState.InputtingRemainingFields)
                    linkEventsReporter.onSignupStarted()
                }
            },
            onFailure = {
                updateSignUpState(SignUpState.InputtingPrimaryField)
                onError(it)
            }
        )
    }

    private fun onError(error: Throwable) {
        logger.error("Error: ", error)
        updateState {
            it.copy(
                errorMessage = when (val errorMessage = error.getErrorMessage()) {
                    is ErrorMessage.FromResources -> {
                        errorMessage.stringResId.resolvableString
                    }
                    is ErrorMessage.Raw -> {
                        errorMessage.errorMessage.resolvableString
                    }
                }
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

        fun factory(
            parentComponent: NativeLinkComponent
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    SignUpViewModel(
                        configuration = parentComponent.configuration,
                        linkEventsReporter = parentComponent.linkEventsReporter,
                        linkAccountManager = parentComponent.linkAccountManager,
                        logger = parentComponent.logger
                    )
                }
            }
        }
    }
}
