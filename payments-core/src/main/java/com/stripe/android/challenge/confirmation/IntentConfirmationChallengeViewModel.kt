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
import com.stripe.android.challenge.confirmation.di.SDK_USER_AGENT
import com.stripe.android.core.injection.UIContext
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.CancelCaptchaChallengeParams
import com.stripe.android.networking.StripeRepository
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.analytics.ErrorReporter.UnexpectedErrorEvent
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
        analyticsEventReporter.onStart()
        super.onStart(owner)
    }

    fun handleWebViewError(error: WebViewError) {
        analyticsEventReporter.onError(
            errorType = error.webViewErrorType,
            errorCode = error.errorCode.toString(),
            fromBridge = false,
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
        viewModelScope.launch {
            val intentId = args.intent.id
            val clientSecret = args.intent.clientSecret

            if (intentId == null || clientSecret == null) {
                errorReporter.report(
                    errorEvent = UnexpectedErrorEvent.INTENT_CONFIRMATION_CHALLENGE_INTENT_PARAMETERS_UNAVAILABLE,
                )
                _result.emit(
                    IntentConfirmationChallengeActivityResult.Failed(
                        clientSecret = clientSecret,
                        error = IllegalArgumentException("Intent parameters are unavailable")
                    )
                )
                return@launch
            }
            val params = CancelCaptchaChallengeParams(clientSecret)

            stripeRepository.cancelCaptchaChallenge(
                intentId = intentId,
                params = params,
                requestOptions = requestOptions,
            ).fold(
                onSuccess = { intent ->
                    _result.emit(
                        IntentConfirmationChallengeActivityResult.Canceled(intent.clientSecret)
                    )
                },
                onFailure = { error ->
                    _result.emit(
                        IntentConfirmationChallengeActivityResult.Failed(
                            clientSecret = args.intent.clientSecret,
                            error = error,
                        )
                    )
                }
            )
        }
    }

    private suspend fun listenToEvents() {
        bridgeHandler.event.collectLatest { event ->
            when (event) {
                is ConfirmationChallengeBridgeEvent.Ready -> {
                    analyticsEventReporter.onWebViewLoaded()
                    _bridgeReady.emit(Unit)
                }
                is ConfirmationChallengeBridgeEvent.Success -> {
                    analyticsEventReporter.onSuccess()
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
                        fromBridge = true
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
