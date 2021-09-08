package com.stripe.android.payments.paymentlauncher

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.StripeIntentResult
import com.stripe.android.exception.APIException
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.AnalyticsEvent
import com.stripe.android.networking.AnalyticsRequestFactory
import com.stripe.android.networking.ApiRequest
import com.stripe.android.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.DefaultReturnUrl
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.payments.PaymentIntentFlowResultProcessor
import com.stripe.android.payments.SetupIntentFlowResultProcessor
import com.stripe.android.payments.core.authentication.PaymentAuthenticatorRegistry
import com.stripe.android.payments.core.injection.Injectable
import com.stripe.android.payments.core.injection.InjectorKey
import com.stripe.android.payments.core.injection.UIContext
import com.stripe.android.payments.core.injection.WeakMapInjectorRegistry
import com.stripe.android.view.AuthActivityStarterHost
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

/**
 * [ViewModel] for [PaymentLauncherConfirmationActivity].
 */

internal class PaymentLauncherViewModel(
    private val stripeApiRepository: StripeRepository,
    private val authenticatorRegistry: PaymentAuthenticatorRegistry,
    private val defaultReturnUrl: DefaultReturnUrl,
    private val apiRequestOptionsProvider: Provider<ApiRequest.Options>,
    private val threeDs1IntentReturnUrlMap: MutableMap<String, String>,
    private val lazyPaymentIntentFlowResultProcessor: dagger.Lazy<PaymentIntentFlowResultProcessor>,
    private val lazySetupIntentFlowResultProcessor: dagger.Lazy<SetupIntentFlowResultProcessor>,
    private val analyticsRequestExecutor: DefaultAnalyticsRequestExecutor,
    private val analyticsRequestFactory: AnalyticsRequestFactory,
    private val uiContext: CoroutineContext,
    private val authActivityStarterHost: AuthActivityStarterHost,
    activityResultCaller: ActivityResultCaller
) : ViewModel() {
    init {
        authenticatorRegistry.onNewActivityResultCaller(
            activityResultCaller,
            ::onPaymentFlowResult
        )
    }

    /**
     * [PaymentResult] live data to be observed.
     */
    internal val paymentLauncherResult = MutableLiveData<PaymentResult>()

    lateinit var stripeIntent: StripeIntent

    /**
     * Confirms a payment intent or setup intent
     */
    internal suspend fun confirmStripeIntent(confirmStripeIntentParams: ConfirmStripeIntentParams) {
        logReturnUrl(confirmStripeIntentParams.returnUrl)
        val returnUrl = confirmStripeIntentParams.returnUrl.takeUnless { it.isNullOrBlank() }
            ?: defaultReturnUrl.value
        runCatching {
            confirmIntent(confirmStripeIntentParams, returnUrl)
        }.fold(
            onSuccess = { intent ->
                intent.nextActionData?.let {
                    if (it is StripeIntent.NextActionData.SdkData.Use3DS1) {
                        intent.id?.let { intentId ->
                            threeDs1IntentReturnUrlMap[intentId] = returnUrl
                        }
                    }
                }
                stripeIntent = intent
                authenticatorRegistry.getAuthenticator(intent).authenticate(
                    authActivityStarterHost,
                    intent,
                    apiRequestOptionsProvider.get()
                )
            },
            onFailure = {
                paymentLauncherResult.postValue(PaymentResult.Failed(it))
            }
        )
    }

    private suspend fun confirmIntent(
        confirmStripeIntentParams: ConfirmStripeIntentParams,
        returnUrl: String
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
    internal suspend fun handleNextActionForStripeIntent(clientSecret: String) {
        runCatching {
            requireNotNull(
                stripeApiRepository.retrieveStripeIntent(
                    clientSecret,
                    apiRequestOptionsProvider.get()
                )
            )
        }.fold(
            onSuccess = { intent ->
                stripeIntent = intent
                authenticatorRegistry.getAuthenticator(intent)
                    .authenticate(
                        authActivityStarterHost,
                        intent,
                        apiRequestOptionsProvider.get()
                    )
            },
            onFailure = {
                paymentLauncherResult.postValue(PaymentResult.Failed(it))
            }
        )
    }

    @VisibleForTesting
    internal fun onPaymentFlowResult(paymentFlowResult: PaymentFlowResult.Unvalidated) {
        viewModelScope.launch {
            runCatching {
                when (stripeIntent) {
                    is PaymentIntent -> {
                        lazyPaymentIntentFlowResultProcessor.get()
                    }
                    is SetupIntent -> {
                        lazySetupIntentFlowResultProcessor.get()
                    }
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
                AnalyticsEvent.ConfirmReturnUrlDefault
            }
            null -> {
                AnalyticsEvent.ConfirmReturnUrlNull
            }
            else -> {
                AnalyticsEvent.ConfirmReturnUrlCustom
            }
        }.let { event ->
            analyticsRequestExecutor.executeAsync(
                analyticsRequestFactory.createRequest(event)
            )
        }
    }

    internal class Factory(
        @InjectorKey private val injectorKeyProvider: () -> Int,
        private val authActivityStarterHostProvider: () -> AuthActivityStarterHost,
        private val activityResultCaller: ActivityResultCaller
    ) : ViewModelProvider.Factory, Injectable<Factory.FallbackInitializeParam> {
        internal data class FallbackInitializeParam(
            val enableLogging: Boolean
        )

        override fun fallbackInitialize(arg: FallbackInitializeParam) {
            // TODO(ccen) to implement
        }

        @Inject
        lateinit var stripeApiRepository: StripeRepository

        @Inject
        lateinit var authenticatorRegistry: PaymentAuthenticatorRegistry

        @Inject
        lateinit var defaultReturnUrl: DefaultReturnUrl

        @Inject
        lateinit var apiRequestOptionsProvider: Provider<ApiRequest.Options>

        @Inject
        lateinit var threeDs1IntentReturnUrlMap: MutableMap<String, String>

        @Inject
        lateinit var lazyPaymentIntentFlowResultProcessor: dagger.Lazy<PaymentIntentFlowResultProcessor>

        @Inject
        lateinit var lazySetupIntentFlowResultProcessor: dagger.Lazy<SetupIntentFlowResultProcessor>

        @Inject
        lateinit var analyticsRequestExecutor: DefaultAnalyticsRequestExecutor

        @Inject
        lateinit var analyticsRequestFactory: AnalyticsRequestFactory

        @Inject
        @UIContext
        lateinit var uiContext: CoroutineContext

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            WeakMapInjectorRegistry.retrieve(injectorKeyProvider())?.inject(this) ?: run {
                throw IllegalArgumentException("Failed to initialize PaymentLauncherViewModel.Factory")
            }

            return PaymentLauncherViewModel(
                stripeApiRepository,
                authenticatorRegistry,
                defaultReturnUrl,
                apiRequestOptionsProvider,
                threeDs1IntentReturnUrlMap,
                lazyPaymentIntentFlowResultProcessor,
                lazySetupIntentFlowResultProcessor,
                analyticsRequestExecutor,
                analyticsRequestFactory,
                uiContext,
                authActivityStarterHostProvider(),
                activityResultCaller
            ) as T
        }
    }

    internal companion object {
        const val TIMEOUT_ERROR = "Payment fails due to time out. \n"
        const val UNKNOWN_ERROR = "Payment fails due to unknown error. \n"
        const val REQUIRED_ERROR = "API request returned an invalid response."
        val EXPAND_PAYMENT_METHOD = listOf("payment_method")
    }
}
