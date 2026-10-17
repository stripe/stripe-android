package com.stripe.android.challenge.confirmation

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.challenge.confirmation.analytics.IntentConfirmationChallengeAnalyticsEventReporter
import com.stripe.android.challenge.confirmation.di.DaggerIntentConfirmationChallengeComponent
import com.stripe.android.challenge.confirmation.di.FireAndForgetScope
import com.stripe.android.challenge.confirmation.di.SDK_USER_AGENT
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.CancelCaptchaChallengeParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.ErrorReporter.ExpectedErrorEvent
import com.stripe.android.payments.core.analytics.ErrorReporter.UnexpectedErrorEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

internal class IntentConfirmationChallengeViewModel @Inject constructor(
    private val args: IntentConfirmationChallengeArgs,
    val bridgeHandler: ConfirmationChallengeBridgeHandler,
    @UIContext private val workContext: CoroutineContext,
    private val analyticsEventReporter: IntentConfirmationChallengeAnalyticsEventReporter,
    @Named(SDK_USER_AGENT) val userAgent: String,
    private val stripeRepository: StripeRepository,
    private val errorReporter: ErrorReporter,
    private val requestOptions: ApiRequest.Options,
    @FireAndForgetScope private val fireAndForgetScope: CoroutineScope,
    val logger: Logger,
) : ViewModel(), DefaultLifecycleObserver {

    private val _bridgeReady = MutableSharedFlow<Unit>()
    val bridgeReady: Flow<Unit> = _bridgeReady

    private val _result = MutableSharedFlow<IntentConfirmationChallengeActivityResult>()
    val result: SharedFlow<IntentConfirmationChallengeActivityResult> = _result

    init {
        viewModelScope.launch(workContext) {
            listenToEvents()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        analyticsEventReporter.onStart(captchaVendorName = args.captchaVendorName)
        super.onStart(owner)
    }

    fun handleWebViewError(error: WebViewError) {
        analyticsEventReporter.onError(
            errorType = error.webViewErrorType,
            errorCode = error.errorCode.toString(),
            fromBridge = false,
            captchaVendorName = args.captchaVendorName,
        )
        viewModelScope.launch {
            _result.emit(
                value = IntentConfirmationChallengeActivityResult.Failed(
                    clientSecret = args.intent.clientSecret,
                    error = error
                )
            )
        }
    }

    fun closeClicked() {
        analyticsEventReporter.onCancel(captchaVendorName = args.captchaVendorName)
        viewModelScope.launch {
            _result.emit(
                IntentConfirmationChallengeActivityResult.Canceled(args.intent.clientSecret)
            )

            fireAndForgetScope.launch {
                cancelChallenge()
            }
        }
    }

    private suspend fun cancelChallenge() {
        val intentId = args.intent.id
        val clientSecret = args.intent.clientSecret

        if (intentId == null || clientSecret == null) {
            errorReporter.report(
                errorEvent = UnexpectedErrorEvent.INTENT_CONFIRMATION_CHALLENGE_INTENT_PARAMETERS_UNAVAILABLE,
            )
            return
        }
        val params = CancelCaptchaChallengeParams(clientSecret)

        val cancellationResult = when (args.intent) {
            is PaymentIntent -> {
                stripeRepository.cancelPaymentIntentCaptchaChallenge(
                    paymentIntentId = intentId,
                    params = params,
                    requestOptions = requestOptions,
                )
            }
            is SetupIntent -> {
                stripeRepository.cancelSetupIntentCaptchaChallenge(
                    setupIntentId = intentId,
                    params = params,
                    requestOptions = requestOptions,
                )
            }
        }

        // Even upon failing to cancel, there is no recovery action to be taken by the client
        // It will however be logged by the API for the merchant
        cancellationResult.onFailure { error ->
            errorReporter.report(
                errorEvent = ExpectedErrorEvent.INTENT_CONFIRMATION_CHALLENGE_CHALLENGE_CANCELLATION_REQUEST_FAILED,
                stripeException = StripeException.create(error)
            )
        }
    }

    private suspend fun listenToEvents() {
        bridgeHandler.event.collectLatest { event ->
            when (event) {
                is ConfirmationChallengeBridgeEvent.Ready -> {
                    analyticsEventReporter.onWebViewLoaded(captchaVendorName = args.captchaVendorName)
                    _bridgeReady.emit(Unit)
                }
                is ConfirmationChallengeBridgeEvent.Success -> {
                    analyticsEventReporter.onSuccess(captchaVendorName = args.captchaVendorName)
                    _result.emit(
                        IntentConfirmationChallengeActivityResult.Success(
                            clientSecret = event.clientSecret
                        )
                    )
                }
                is ConfirmationChallengeBridgeEvent.Error -> {
                    analyticsEventReporter.onError(
                        errorType = event.error.type,
                        errorCode = event.error.code,
                        fromBridge = true,
                        captchaVendorName = args.captchaVendorName
                    )
                    _result.emit(
                        IntentConfirmationChallengeActivityResult.Failed(
                            error = event.error,
                            clientSecret = args.intent.clientSecret
                        )
                    )
                }
            }
        }
    }

    companion object {
        fun factory(savedStateHandle: SavedStateHandle? = null): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val handle: SavedStateHandle = savedStateHandle ?: createSavedStateHandle()
                val app = this[APPLICATION_KEY] as Application
                val args: IntentConfirmationChallengeArgs = IntentConfirmationChallengeActivity.getArgs(handle)
                    ?: throw IllegalArgumentException("No IntentConfirmationChallengeArgs found")

                DaggerIntentConfirmationChallengeComponent
                    .factory()
                    .create(
                        context = app,
                        args = args,
                    )
                    .viewModel
            }
        }
    }
}
