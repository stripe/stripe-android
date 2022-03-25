package com.stripe.android.payments.paymentlauncher

import android.app.Application
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.stripe.android.StripeIntentResult
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
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
import com.stripe.android.payments.core.injection.PaymentLauncherViewModelSubcomponent
import com.stripe.android.view.AuthActivityStarterHost
import dagger.Lazy
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
    args: PaymentLauncherContract.Args,
    @Named(IS_PAYMENT_INTENT) private val isPaymentIntent: Boolean,
    private val stripeApiRepository: StripeRepository,
    private val authenticatorRegistry: PaymentAuthenticatorRegistry,
    private val defaultReturnUrl: DefaultReturnUrl,
    private val apiRequestOptionsProvider: Provider<ApiRequest.Options>,
    private val threeDs1IntentReturnUrlMap: MutableMap<String, String>,
    private val lazyPaymentIntentFlowResultProcessor: Lazy<PaymentIntentFlowResultProcessor>,
    private val lazySetupIntentFlowResultProcessor: Lazy<SetupIntentFlowResultProcessor>,
    private val analyticsRequestExecutor: DefaultAnalyticsRequestExecutor,
    private val paymentAnalyticsRequestFactory: PaymentAnalyticsRequestFactory,
    @UIContext private val uiContext: CoroutineContext,
    private val savedStateHandle: SavedStateHandle,
    @Named(IS_INSTANT_APP) private val isInstantApp: Boolean
) : ViewModel() {
    init {
        if (!hasStarted) {
            when (args) {
                is PaymentLauncherContract.Args.IntentConfirmationArgs -> {
                    confirmStripeIntent(args.confirmStripeIntentParams)
                }
                is PaymentLauncherContract.Args.PaymentIntentNextActionArgs -> {
                    handleNextActionForStripeIntent(args.paymentIntentClientSecret)
                }
                is PaymentLauncherContract.Args.SetupIntentNextActionArgs -> {
                    handleNextActionForStripeIntent(args.setupIntentClientSecret)
                }
            }
        }
    }

    /**
     * Indicates if the [ViewModel] has complete handling a [StripeIntent].
     * In cases when an [StripeIntent] is still being handled and the
     * [PaymentLauncherConfirmationActivity] is recreated(e.g due to process is killed by system),
     * this value will be set true, preventing [PaymentLauncherConfirmationActivity] to try to
     * confirm the same [StripeIntent] again.
     */
    private val hasStarted: Boolean
        get() = savedStateHandle.get(KEY_HAS_STARTED) ?: false

    /**
     * Set when activity calls [register]. Used to redirect back to calling activity.
     */
    private var authActivityStarterHost: AuthActivityStarterHost? = null

    /**
     * [PaymentResult] live data to be observed.
     */
    internal val paymentLauncherResult = MutableLiveData<PaymentResult>()

    /**
     * Registers the calling activity to listen to payment flow results. Should be called in the
     * activity onCreate.
     */
    internal fun register(caller: ActivityResultCaller, host: AuthActivityStarterHost) {
        authActivityStarterHost = host
        authenticatorRegistry.onNewActivityResultCaller(
            caller,
            ::onPaymentFlowResult
        )
    }

    /**
     * Confirms a payment intent or setup intent
     */
    @VisibleForTesting
    internal fun confirmStripeIntent(confirmStripeIntentParams: ConfirmStripeIntentParams) {
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
            runCatching {
                confirmIntent(confirmStripeIntentParams, returnUrl)
            }.fold(
                onSuccess = { intent ->
                    intent.nextActionData?.let {
                        if (it is StripeIntent.NextActionData.SdkData.Use3DS1) {
                            intent.id?.let { intentId ->
                                threeDs1IntentReturnUrlMap[intentId] = returnUrl.orEmpty()
                            }
                        }
                    }
                    authActivityStarterHost?.let {
                        authenticatorRegistry.getAuthenticator(intent).authenticate(
                            it,
                            intent,
                            apiRequestOptionsProvider.get()
                        )
                    }
                },
                onFailure = {
                    paymentLauncherResult.postValue(PaymentResult.Failed(it))
                }
            )
        }
    }

    private suspend fun confirmIntent(
        confirmStripeIntentParams: ConfirmStripeIntentParams,
        returnUrl: String?
    ): StripeIntent =
        confirmStripeIntentParams.also {
            it.returnUrl = returnUrl
        }.withShouldUseStripeSdk(shouldUseStripeSdk = true).let { decoratedParams ->
            requireNotNull(
                when (decoratedParams) {
                    is ConfirmPaymentIntentParams -> {
                        stripeApiRepository.confirmPaymentIntent(
                            decoratedParams,
                            apiRequestOptionsProvider.get(),
                            expandFields = EXPAND_PAYMENT_METHOD
                        )
                    }
                    is ConfirmSetupIntentParams -> {
                        stripeApiRepository.confirmSetupIntent(
                            decoratedParams,
                            apiRequestOptionsProvider.get(),
                            expandFields = EXPAND_PAYMENT_METHOD
                        )
                    }
                }
            ) {
                REQUIRED_ERROR
            }
        }

    /**
     * Fetches a [StripeIntent] and handles its next action.
     */
    internal fun handleNextActionForStripeIntent(clientSecret: String) {
        viewModelScope.launch {
            savedStateHandle.set(KEY_HAS_STARTED, true)
            runCatching {
                requireNotNull(
                    stripeApiRepository.retrieveStripeIntent(
                        clientSecret,
                        apiRequestOptionsProvider.get()
                    )
                )
            }.fold(
                onSuccess = { intent ->
                    authActivityStarterHost?.let {
                        authenticatorRegistry
                            .getAuthenticator(intent)
                            .authenticate(
                                it,
                                intent,
                                apiRequestOptionsProvider.get()
                            )
                    }
                },
                onFailure = {
                    paymentLauncherResult.postValue(PaymentResult.Failed(it))
                }
            )
        }
    }

    @VisibleForTesting
    internal fun onPaymentFlowResult(paymentFlowResult: PaymentFlowResult.Unvalidated) {
        viewModelScope.launch {
            runCatching {
                if (isPaymentIntent) {
                    lazyPaymentIntentFlowResultProcessor.get()
                } else {
                    lazySetupIntentFlowResultProcessor.get()
                }.processResult(paymentFlowResult)
            }.fold(
                onSuccess = {
                    withContext(uiContext) {
                        postResult(it)
                    }
                },
                onFailure = {
                    withContext(uiContext) {
                        paymentLauncherResult.postValue(PaymentResult.Failed(it))
                    }
                }
            )
        }
    }

    /**
     * Cleans up the [PaymentAuthenticatorRegistry] by invalidating [ActivityResultLauncher]s
     * registered within.
     *
     * Because the same [PaymentAuthenticatorRegistry] is used for multiple
     * [PaymentLauncherConfirmationActivity]s. The [ActivityResultLauncher]s registered in the old
     * [PaymentLauncherConfirmationActivity] needs to be unregistered to prevent leaking.
     */
    internal fun cleanUp() {
        authenticatorRegistry.onLauncherInvalidated()
    }

    /**
     * Parse [StripeIntentResult] into [PaymentResult].
     */
    private fun postResult(stripeIntentResult: StripeIntentResult<StripeIntent>) {
        paymentLauncherResult.postValue(
            when (stripeIntentResult.outcome) {
                StripeIntentResult.Outcome.SUCCEEDED ->
                    PaymentResult.Completed
                StripeIntentResult.Outcome.FAILED ->
                    PaymentResult.Failed(
                        APIException(message = stripeIntentResult.failureMessage)
                    )
                StripeIntentResult.Outcome.CANCELED ->
                    PaymentResult.Canceled
                StripeIntentResult.Outcome.TIMEDOUT ->
                    PaymentResult.Failed(
                        APIException(message = TIMEOUT_ERROR + stripeIntentResult.failureMessage)
                    )
                else ->
                    PaymentResult.Failed(
                        APIException(message = UNKNOWN_ERROR + stripeIntentResult.failureMessage)
                    )
            }
        )
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
        private val applicationSupplier: () -> Application,
        owner: SavedStateRegistryOwner,
    ) : AbstractSavedStateViewModelFactory(owner, null),
        Injectable<Factory.FallbackInitializeParam> {
        internal data class FallbackInitializeParam(
            val application: Application,
            val enableLogging: Boolean,
            val publishableKey: String,
            val stripeAccountId: String?,
            val productUsage: Set<String>
        )

        @Inject
        lateinit var subComponentBuilderProvider: Provider<PaymentLauncherViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T {
            val arg = argsSupplier()
            injectWithFallback(
                arg.injectorKey,
                FallbackInitializeParam(
                    applicationSupplier(),
                    arg.enableLogging,
                    arg.publishableKey,
                    arg.stripeAccountId,
                    arg.productUsage
                )
            )

            return subComponentBuilderProvider.get()
                .configuration(argsSupplier())
                .isPaymentIntent(
                    when (arg) {
                        is PaymentLauncherContract.Args.IntentConfirmationArgs -> {
                            when (arg.confirmStripeIntentParams) {
                                is ConfirmPaymentIntentParams -> true
                                is ConfirmSetupIntentParams -> false
                            }
                        }
                        is PaymentLauncherContract.Args.PaymentIntentNextActionArgs -> true
                        is PaymentLauncherContract.Args.SetupIntentNextActionArgs -> false
                    }
                )
                .savedStateHandle(handle)
                .build().viewModel as T
        }

        /**
         * Fallback call to initialize dependencies when injection is not available, this might happen
         * when app process is killed by system and [WeakMapInjectorRegistry] is cleared.
         */
        override fun fallbackInitialize(arg: FallbackInitializeParam) {
            DaggerPaymentLauncherViewModelFactoryComponent.builder()
                .context(arg.application)
                .enableLogging(arg.enableLogging)
                .publishableKeyProvider { arg.publishableKey }
                .stripeAccountIdProvider { arg.stripeAccountId }
                .productUsage(arg.productUsage)
                .build().inject(this)
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
