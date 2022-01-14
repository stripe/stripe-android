package com.stripe.android.link.ui.signup

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.injection.DaggerSignUpViewModelFactoryComponent
import com.stripe.android.link.injection.SignUpViewModelSubcomponent
import com.stripe.android.link.repositories.LinkRepository
import com.stripe.android.ui.core.elements.EmailSpec
import com.stripe.android.ui.core.elements.SectionFieldElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

/**
 * ViewModel that handles user sign up logic.
 */
internal class SignUpViewModel @Inject internal constructor(
    @IOContext private val workContext: CoroutineContext,
    args: LinkActivityContract.Args,
    private val linkRepository: LinkRepository,
    private val logger: Logger
) : ViewModel() {
    val merchantName: String = args.merchantName

    val emailElement: SectionFieldElement = EmailSpec.transform(args.customerEmail)

    /**
     * Emits the email entered in the form if valid, null otherwise.
     */
    val consumerEmail: Flow<String?> = emailElement.getFormFieldValueFlow().map {
        it.firstOrNull()?.second?.let { formFieldEntry ->
            if (formFieldEntry.isComplete) {
                formFieldEntry.value
            } else {
                null
            }
        }
    }

    private val _signUpStatus = MutableStateFlow(SignUpStatus.InputtingEmail)
    val signUpStatus: Flow<SignUpStatus> = _signUpStatus

    // Holds a Job that looks up the email after a delay, so that we can cancel it
    private var lookupJob: Job? = null

    init {
        viewModelScope.launch {
            consumerEmail.collect { email ->
                lookupJob?.cancel()

                if (email != null) {
                    lookupJob = CoroutineScope(workContext).launch {
                        delay(LOOKUP_DEBOUNCE_MS)
                        if (isActive) {
                            _signUpStatus.value = SignUpStatus.VerifyingEmail
                            lookupConsumerEmail(email)
                        }
                    }
                } else {
                    SignUpStatus.InputtingEmail
                }
            }
        }
    }

    private suspend fun lookupConsumerEmail(email: String) {
        linkRepository.lookupConsumer(email)?.let {
            if (it.consumerSession != null) {
                // TODO(brnunes): Trigger verification
            } else {
                _signUpStatus.value = SignUpStatus.InputtingPhone
            }
        } ?: onError("")
    }

    private fun onError(errorMessage: String) {
        logger.error(errorMessage)
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
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
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
                .build().viewModel as T
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
        private const val LOOKUP_DEBOUNCE_MS = 300L
    }
}

internal enum class SignUpStatus {
    InputtingEmail,
    VerifyingEmail,
    InputtingPhone
}
