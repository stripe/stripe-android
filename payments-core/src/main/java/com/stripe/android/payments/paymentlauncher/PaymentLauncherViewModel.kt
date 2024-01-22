package com.stripe.android.payments.paymentlauncher

import androidx.activity.result.ActivityResultCaller
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.StripeIntentResult
import com.stripe.android.core.exception.LocalStripeException
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.PaymentAnalyticsEvent
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.PaymentIntentFlowResultProcessor
import com.stripe.android.payments.SetupIntentFlowResultProcessor
import com.stripe.android.payments.core.authentication.PaymentAuthenticatorRegistry
import com.stripe.android.payments.core.injection.DaggerPaymentLauncherViewModelFactoryComponent
import com.stripe.android.payments.core.injection.IS_INSTANT_APP
import com.stripe.android.payments.core.injection.IS_PAYMENT_INTENT
import com.stripe.android.utils.requireApplication
import com.stripe.android.view.AuthActivityStarterHost
import dagger.Lazy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

/**
 * [ViewModel] for [PaymentLauncherConfirmationActivity].
 */
internal class PaymentLauncherViewModel @Inject constructor(
    @Named(IS_PAYMENT_INTENT) private val isPaymentIntent: Boolean,
    private val stripeApiRepository: StripeRepository,
    private val authenticatorRegistry: PaymentAuthenticatorRegistry,
    private val defaultReturnUrl: DefaultReturnUrl,
    private val apiRequestOptionsProvider: Provider<ApiRequest.Options>,
    private val threeDs1IntentReturnUrlMap: MutableMap<String, String>,
    private val lazyPaymentIntentFlowResultProcessor: Lazy<PaymentIntentFlowResultProcessor>,
    private val lazySetupIntentFlowResultProcessor: Lazy<SetupIntentFlowResultProcessor>,
    private val analyticsRequestExecutor: AnalyticsRequestExecutor,
    private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
    @UIContext private val uiContext: CoroutineContext,
    private val savedStateHandle: SavedStateHandle,
    @Named(IS_INSTANT_APP) private val isInstantApp: Boolean
) : ViewModel() {

    /**
     * Indicates if the [ViewModel] has complete handling a [StripeIntent].
     * In cases when an [StripeIntent] is still being handled and the
     * [PaymentLauncherConfirmationActivity] is recreated(e.g due to process is killed by system),
     * this value will be set true, preventing [PaymentLauncherConfirmationActivity] to try to
     * confirm the same [StripeIntent] again.
     */
    private val hasStarted: Boolean
        get() = savedStateHandle.get(KEY_HAS_STARTED) ?: false

    internal val internalPaymentResult = MutableStateFlow<InternalPaymentResult?>(null)

    /**
     * Registers the calling activity to listen to payment flow results. Should be called in the
     * activity onCreate.
     */
    internal fun register(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
    ) {
        authenticatorRegistry.onNewActivityResultCaller(
            activityResultCaller = activityResultCaller,
            activityResultCallback = ::onPaymentFlowResult,
        )

        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    authenticatorRegistry.onLauncherInvalidated()
                    super.onDestroy(owner)
                }
            }
        )
    }

    /**
     * Confirms a payment intent or setup intent
     */
    internal fun confirmStripeIntent(
        confirmStripeIntentParams: ConfirmStripeIntentParams,
        host: AuthActivityStarterHost
    ) {
        if (hasStarted) return
        viewModelScope.launch {
            savedStateHandle.set(KEY_HAS_STARTED, true)
            logReturnUrl(confirmStripeIntentParams.returnUrl)
            val returnUrl =
                if (isInstantApp) {
                    confirmStripeIntentParams.returnUrl
                } else {
                    confirmStripeIntentParams.returnUrl.takeUnless { it.isNullOrBlank() }
                        ?: defaultReturnUrl.value
                }

            confirmIntent(confirmStripeIntentParams, returnUrl).fold(
                onSuccess = { intent ->
                    intent.nextActionData?.let {
                        if (it is StripeIntent.NextActionData.SdkData.Use3DS1) {
                            intent.id?.let { intentId ->
                                threeDs1IntentReturnUrlMap[intentId] = returnUrl.orEmpty()
                            }
                        }
                    }
                    if (!intent.requiresAction()) {
                        withContext(uiContext) {
                            internalPaymentResult.value = InternalPaymentResult.Completed(intent)
                        }
                    } else {
                        authenticatorRegistry.getAuthenticator(intent).authenticate(
                            host,
                            intent,
                            apiRequestOptionsProvider.get()
                        )
                    }
                },
                onFailure = {
                    withContext(uiContext) {
                        internalPaymentResult.value = InternalPaymentResult.Failed(it)
                    }
                }
            )
        }
    }

    private suspend fun confirmIntent(
        confirmStripeIntentParams: ConfirmStripeIntentParams,
        returnUrl: String?
    ): Result<StripeIntent> {
        val decoratedParams = confirmStripeIntentParams.also {
            it.returnUrl = returnUrl
        }.withShouldUseStripeSdk(true)

        return when (decoratedParams) {
            is ConfirmPaymentIntentParams -> {
                stripeApiRepository.confirmPaymentIntent(
                    confirmPaymentIntentParams = decoratedParams,
                    options = apiRequestOptionsProvider.get(),
                    expandFields = EXPAND_PAYMENT_METHOD,
                )
            }
            is ConfirmSetupIntentParams -> {
                stripeApiRepository.confirmSetupIntent(
                    confirmSetupIntentParams = decoratedParams,
                    options = apiRequestOptionsProvider.get(),
                    expandFields = EXPAND_PAYMENT_METHOD,
                )
            }
        }
    }

    /**
     * Fetches a [StripeIntent] and handles its next action.
     */
    internal fun handleNextActionForStripeIntent(clientSecret: String, host: AuthActivityStarterHost) {
        if (hasStarted) return
        viewModelScope.launch {
            savedStateHandle.set(KEY_HAS_STARTED, true)

            stripeApiRepository.retrieveStripeIntent(
                clientSecret = clientSecret,
                options = apiRequestOptionsProvider.get(),
            ).fold(
                onSuccess = { intent ->
                    authenticatorRegistry
                        .getAuthenticator(intent)
                        .authenticate(
                            host,
                            intent,
                            apiRequestOptionsProvider.get()
                        )
                },
                onFailure = {
                    withContext(uiContext) {
                        internalPaymentResult.value = InternalPaymentResult.Failed(it)
                    }
                }
            )
        }
    }

    @VisibleForTesting
    internal fun onPaymentFlowResult(paymentFlowResult: PaymentFlowResult.Unvalidated) {
        viewModelScope.launch {
            val resultProcessor = if (isPaymentIntent) {
                lazyPaymentIntentFlowResultProcessor.get()
            } else {
                lazySetupIntentFlowResultProcessor.get()
            }

            resultProcessor.processResult(paymentFlowResult).fold(
                onSuccess = {
                    withContext(uiContext) {
                        postResult(it)
                    }
                },
                onFailure = {
                    withContext(uiContext) {
                        internalPaymentResult.value = InternalPaymentResult.Failed(it)
                    }
                }
            )
        }
    }

    /**
     * Parse [StripeIntentResult] into [PaymentResult].
     */
    private fun postResult(stripeIntentResult: StripeIntentResult<StripeIntent>) {
        internalPaymentResult.value =
            when (stripeIntentResult.outcome) {
                StripeIntentResult.Outcome.SUCCEEDED ->
                    InternalPaymentResult.Completed(stripeIntentResult.intent)
                StripeIntentResult.Outcome.FAILED ->
                    InternalPaymentResult.Failed(
                        LocalStripeException(
                            displayMessage = stripeIntentResult.failureMessage,
                            analyticsValue = "failedIntentOutcomeError",
                        )
                    )
                StripeIntentResult.Outcome.CANCELED ->
                    InternalPaymentResult.Canceled
                StripeIntentResult.Outcome.TIMEDOUT ->
                    InternalPaymentResult.Failed(
                        LocalStripeException(
                            displayMessage = TIMEOUT_ERROR + stripeIntentResult.failureMessage,
                            analyticsValue = "timedOutIntentOutcomeError",
                        )
                    )
                else ->
                    InternalPaymentResult.Failed(
                        LocalStripeException(
                            displayMessage = UNKNOWN_ERROR + stripeIntentResult.failureMessage,
                            analyticsValue = "unknownIntentOutcomeError",
                        )
                    )
            }
    }

    private fun logReturnUrl(returnUrl: String?) {
        when (returnUrl) {
            defaultReturnUrl.value -> {
                PaymentAnalyticsEvent.ConfirmReturnUrlDefault
            }
            null -> {
                PaymentAnalyticsEvent.ConfirmReturnUrlNull
            }
            else -> {
                PaymentAnalyticsEvent.ConfirmReturnUrlCustom
            }
        }.let { event ->
            analyticsRequestExecutor.executeAsync(
                paymentAnalyticsRequestFactory.createRequest(event)
            )
        }
    }

    internal class Factory(
        private val argsSupplier: () -> PaymentLauncherContract.Args,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val arg = argsSupplier()

            val application = extras.requireApplication()
            val savedStateHandle = extras.createSavedStateHandle()

            val subcomponentBuilder = DaggerPaymentLauncherViewModelFactoryComponent.builder()
                .context(application)
                .enableLogging(arg.enableLogging)
                .publishableKeyProvider { arg.publishableKey }
                .stripeAccountIdProvider { arg.stripeAccountId }
                .productUsage(arg.productUsage)
                .includePaymentSheetAuthenticators(arg.includePaymentSheetAuthenticators)
                .build().viewModelSubcomponentBuilder

            val isPaymentIntent = when (arg) {
                is PaymentLauncherContract.Args.IntentConfirmationArgs -> {
                    when (arg.confirmStripeIntentParams) {
                        is ConfirmPaymentIntentParams -> true
                        is ConfirmSetupIntentParams -> false
                    }
                }
                is PaymentLauncherContract.Args.PaymentIntentNextActionArgs -> true
                is PaymentLauncherContract.Args.SetupIntentNextActionArgs -> false
            }

            return subcomponentBuilder
                .isPaymentIntent(isPaymentIntent)
                .savedStateHandle(savedStateHandle)
                .build().viewModel as T
        }
    }

    internal companion object {
        const val TIMEOUT_ERROR = "Payment fails due to time out. \n"
        const val UNKNOWN_ERROR = "Payment fails due to unknown error. \n"
        const val REQUIRED_ERROR = "API request returned an invalid response."
        val EXPAND_PAYMENT_METHOD = listOf("payment_method")

        @VisibleForTesting
        internal const val KEY_HAS_STARTED = "key_has_started"
    }
}
