package com.stripe.android.link.ui.signup

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkScreen
import com.stripe.android.link.injection.DaggerSignUpViewModelFactoryComponent
import com.stripe.android.link.injection.SignUpViewModelSubcomponent
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.model.Navigator
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.model.ConsumerSession
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.SectionFieldElement
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

/**
 * ViewModel that handles user sign up logic.
 */
internal class SignUpViewModel @Inject internal constructor(
    args: LinkActivityContract.Args,
    private val linkRepository: LinkRepository,
    private val navigator: Navigator,
    private val logger: Logger
) : ViewModel() {
    val merchantName: String = args.merchantName

    val emailElement: SectionFieldElement = EmailSpec.transform(args.customerEmail)

    /**
     * Emits the email entered in the form if valid, null otherwise.
     */
    private val consumerEmail: StateFlow<String?> = emailElement.getFormFieldValueFlow().map {
        it.firstOrNull()?.second?.let { formFieldEntry ->
            if (formFieldEntry.isComplete) {
                formFieldEntry.value
            } else {
                null
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, args.customerEmail)

    private val _signUpStatus = MutableStateFlow(SignUpStatus.InputtingEmail)
    val signUpStatus: StateFlow<SignUpStatus> = _signUpStatus

    /**
     * Holds a Job that looks up the email after a delay, so that we can cancel it if the user is
     * still typing.
     */
    private var lookupJob: Job? = null

    init {
        viewModelScope.launch {
            consumerEmail.collect { email ->
                // The first emitted value is the one provided in the arguments, and shouldn't
                // trigger a lookup
                if (email == args.customerEmail && lookupJob == null) {
                    // If it's a valid email, collect phone number
                    if (email != null) {
                        _signUpStatus.value = SignUpStatus.InputtingPhone
                    }
                    return@collect
                }

                lookupJob?.cancel()

                if (email != null) {
                    lookupJob = launch {
                        delay(LOOKUP_DEBOUNCE_MS)
                        if (isActive) {
                            _signUpStatus.value = SignUpStatus.VerifyingEmail
                            lookupConsumerEmail(email)
                        }
                    }
                } else {
                    _signUpStatus.value = SignUpStatus.InputtingEmail
                }
            }
        }
    }

    fun onSignUpClick(phone: String) {
        // Email must be valid otherwise sign up button would not be displayed
        val email = requireNotNull(consumerEmail.value)
        viewModelScope.launch {
            // TODO(brnunes-stripe): Read formatted phone and country code from phone number element
            linkRepository.consumerSignUp(email, "+1$phone", "US")?.let {
                onConsumerSessionResult(it)
            } ?: onError("")
        }
    }

    private suspend fun lookupConsumerEmail(email: String) {
        linkRepository.lookupConsumer(email)?.let { consumerSessionLookup ->
            val consumerSession = consumerSessionLookup.consumerSession
            if (consumerSession != null) {
                onConsumerSessionResult(consumerSession)
            } else {
                _signUpStatus.value = SignUpStatus.InputtingPhone
            }
        } ?: onError("")
    }

    private fun onConsumerSessionResult(consumerSession: ConsumerSession) {
        navigator.navigateTo(
            if (LinkAccount(consumerSession).isVerified) {
                LinkScreen.Wallet
            } else {
                LinkScreen.Verification
            }
        )
    }

    private fun onError(errorMessage: String) {
        logger.error(errorMessage)
        // TODO(brnunes-stripe): Add localized error messages, show them in UI.
    }

    internal class Factory(
        private val application: Application,
        private val starterArgsSupplier: () -> LinkActivityContract.Args
    ) : ViewModelProvider.Factory, Injectable<Factory.FallbackInitializeParam> {
        internal data class FallbackInitializeParam(
            val application: Application,
            val enableLogging: Boolean,
            val publishableKey: String,
            val stripeAccountId: String?,
            val productUsage: Set<String>
        )

        @Inject
        lateinit var subComponentBuilderProvider:
            Provider<SignUpViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val args = starterArgsSupplier()
            injectWithFallback(
                args.injectionParams?.injectorKey,
                FallbackInitializeParam(
                    application,
                    args.injectionParams?.enableLogging ?: false,
                    args.injectionParams?.publishableKey
                        ?: PaymentConfiguration.getInstance(application).publishableKey,
                    if (args.injectionParams != null) {
                        args.injectionParams.stripeAccountId
                    } else {
                        PaymentConfiguration.getInstance(application).stripeAccountId
                    },
                    args.injectionParams?.productUsage ?: emptySet()
                )
            )
            return subComponentBuilderProvider.get()
                .args(args)
                .build().signUpViewModel as T
        }

        override fun fallbackInitialize(arg: FallbackInitializeParam) {
            DaggerSignUpViewModelFactoryComponent.builder()
                .context(arg.application)
                .enableLogging(arg.enableLogging)
                .publishableKeyProvider { arg.publishableKey }
                .stripeAccountIdProvider { arg.stripeAccountId }
                .productUsage(arg.productUsage)
                .build().inject(this)
        }
    }

    companion object {
        // How long to wait (in milliseconds) before triggering a call to lookup the email
        const val LOOKUP_DEBOUNCE_MS = 700L
    }
}

internal enum class SignUpStatus {
    InputtingEmail,
    VerifyingEmail,
    InputtingPhone
}
