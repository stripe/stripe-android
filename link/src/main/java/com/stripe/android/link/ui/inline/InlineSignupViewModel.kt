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
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.SectionFieldElement
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

    val emailElement: SectionFieldElement =
        EmailSpec.transform(mapOf(IdentifierSpec.Email to prefilledEmail))

    /**
     * Emits the email entered in the form if valid, null otherwise.
     */
    private val consumerEmail: StateFlow<String?> =
        emailElement.getFormFieldValueFlow().map { formFieldsList ->
            // formFieldsList contains only one element, for the email. Take the second value of
            // the pair, which is the FormFieldEntry containing the value entered by the user.
            formFieldsList.firstOrNull()?.second?.takeIf { it.isComplete }?.value
        }.stateIn(viewModelScope, SharingStarted.Lazily, prefilledEmail)

    private val _signUpStatus = MutableStateFlow(SignUpState.InputtingEmail)
    val signUpState: StateFlow<SignUpState> = _signUpStatus

    val isExpanded = MutableStateFlow(false)

    /**
     * Whether we have enough information to proceed with the payment flow.
     * This will be true when the user has entered an email that already has a link account and just
     * needs verification, or when they entered a new email and phone number.
     */
    val isReady = MutableStateFlow(false)
    private var hasExpanded = false

    private var debouncer = SignUpViewModel.Debouncer(prefilledEmail)

    fun toggleExpanded() {
        isExpanded.value = !isExpanded.value
        // First time user checks the box, start listening to email input
        if (isExpanded.value && !hasExpanded) {
            hasExpanded = true
            debouncer.startWatching(
                coroutineScope = viewModelScope,
                emailFlow = consumerEmail,
                onStateChanged = {
                    _signUpStatus.value = it
                    if (it == SignUpState.InputtingEmail || it == SignUpState.InputtingPhone) {
                        isReady.value = false
                    }
                },
                onValidEmailEntered = {
                    viewModelScope.launch {
                        lookupConsumerEmail(it)
                    }
                }
            )
        }
    }

    fun onPhoneInputCompleted(phoneNumber: String?) {
        isReady.value = phoneNumber != null
    }

    private suspend fun lookupConsumerEmail(email: String) {
        linkAccountManager.lookupConsumer(email, startVerification = false).fold(
            onSuccess = {
                if (it != null) {
                    isReady.value = true
                    _signUpStatus.value = SignUpState.InputtingEmail
                } else {
                    isReady.value = false
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
