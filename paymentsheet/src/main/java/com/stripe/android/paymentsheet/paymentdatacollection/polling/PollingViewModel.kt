package com.stripe.android.paymentsheet.paymentdatacollection.polling

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.StripeIntentResult
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.core.injection.Injectable
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.injectWithFallback
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.PaymentFlowResult
import com.stripe.android.paymentsheet.paymentdatacollection.polling.di.DaggerPollingComponent
import com.stripe.android.paymentsheet.paymentdatacollection.polling.di.PollingViewModelSubcomponent
import com.stripe.android.polling.IntentStatusPoller
import com.stripe.android.utils.requireApplication
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val KEY_CURRENT_POLLING_START_TIME = "KEY_CURRENT_POLLING_START_TIME"

internal interface TimeProvider {
    fun currentTimeInMillis(): Long
}

internal class DefaultTimeProvider @Inject constructor() : TimeProvider {
    override fun currentTimeInMillis(): Long {
        return SystemClock.elapsedRealtime()
    }
}

internal enum class PollingState {
    Active,
    Success,
    Failed,
    Canceled,
}

internal fun PollingState.toFlowResult(
    args: PollingContract.Args,
): PaymentFlowResult.Unvalidated? {
    return when (this) {
        PollingState.Active -> {
            null
        }
        PollingState.Failed -> {
            null
        }
        PollingState.Success -> {
            PaymentFlowResult.Unvalidated(
                clientSecret = args.clientSecret,
                flowOutcome = StripeIntentResult.Outcome.SUCCEEDED,
            )
        }
        PollingState.Canceled -> {
            PaymentFlowResult.Unvalidated(
                clientSecret = args.clientSecret,
                flowOutcome = StripeIntentResult.Outcome.CANCELED,
                canCancelSource = false,
            )
        }
    }
}

internal data class PollingUiState(
    val durationRemaining: Duration,
    val pollingState: PollingState = PollingState.Active,
)

internal class PollingViewModel @Inject constructor(
    private val args: Args,
    private val poller: IntentStatusPoller,
    private val timeProvider: TimeProvider,
    private val dispatcher: CoroutineDispatcher,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PollingUiState(durationRemaining = args.timeLimit))
    val uiState: StateFlow<PollingUiState> = _uiState

    init {
        val timeRemaining = computeTimeRemaining()

        viewModelScope.launch(dispatcher) {
            observeCountdown(timeRemaining)
        }

        viewModelScope.launch(dispatcher) {
            observePollingResults()
        }

        viewModelScope.launch(dispatcher) {
            delay(timeRemaining)
            handleTimeLimitReached()
        }

        viewModelScope.launch(dispatcher) {
            delay(args.initialDelay)
            poller.startPolling(scope = this)
        }
    }

    private suspend fun handleTimeLimitReached() {
        poller.stopPolling()
        delay(3.seconds)
        performOneOffPoll()
    }

    private suspend fun performOneOffPoll() {
        val intentStatus = poller.forcePoll()
        if (intentStatus == StripeIntent.Status.Succeeded) {
            _uiState.update {
                it.copy(pollingState = PollingState.Success)
            }
        } else {
            _uiState.update {
                it.copy(pollingState = PollingState.Failed)
            }
        }
    }

    private fun computeTimeRemaining(): Duration {
        val originalStartTime = savedStateHandle.get<Long>(KEY_CURRENT_POLLING_START_TIME)

        if (originalStartTime == null) {
            savedStateHandle[KEY_CURRENT_POLLING_START_TIME] = timeProvider.currentTimeInMillis()
        }

        return if (originalStartTime != null) {
            // We were polling before the process death, so let's use the previous attempt's
            // start time.
            val deadline = originalStartTime + args.timeLimit.inWholeMilliseconds
            val currentTimeInMillis = timeProvider.currentTimeInMillis()
            val millisRemaining = deadline - currentTimeInMillis
            maxOf(millisRemaining.milliseconds, ZERO)
        } else {
            args.timeLimit
        }
    }

    fun pausePolling() {
        poller.stopPolling()
    }

    fun resumePolling() {
        viewModelScope.launch(dispatcher) {
            delay(args.initialDelay)
            poller.startPolling(viewModelScope)
        }
    }

    fun handleCancel() {
        _uiState.update {
            it.copy(pollingState = PollingState.Canceled)
        }
        poller.stopPolling()
    }

    private suspend fun observeCountdown(timeLimit: Duration) {
        countdownFlow(timeLimit).collect { duration ->
            _uiState.update {
                it.copy(durationRemaining = duration)
            }
        }
    }

    private suspend fun observePollingResults() {
        poller.state
            .map { intentStatus ->
                intentStatus?.toPollingState() ?: PollingState.Active
            }
            .onEach { pollingState ->
                if (pollingState == PollingState.Failed) {
                    poller.stopPolling()
                }
            }
            .collect(this::updatePollingState)
    }

    private fun updatePollingState(pollingState: PollingState) {
        _uiState.update {
            it.copy(pollingState = pollingState)
        }
    }

    internal class Factory(
        private val argsSupplier: () -> Args,
    ) : ViewModelProvider.Factory, Injectable<Factory.FallbackInitializeParam> {
        internal data class FallbackInitializeParam(
            val application: Application
        )

        @Inject
        lateinit var subcomponentBuilderProvider: Provider<PollingViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val args = argsSupplier()

            val application = extras.requireApplication()
            val savedStateHandle = extras.createSavedStateHandle()

            injectWithFallback(args.injectorKey, FallbackInitializeParam(application))

            return subcomponentBuilderProvider.get()
                .args(args)
                .savedStateHandle(savedStateHandle)
                .build()
                .viewModel as T
        }

        override fun fallbackInitialize(arg: FallbackInitializeParam): Injector? {
            val args = argsSupplier()

            val config = IntentStatusPoller.Config(
                clientSecret = args.clientSecret,
                maxAttempts = args.maxAttempts,
            )

            DaggerPollingComponent
                .builder()
                .application(arg.application)
                .injectorKey(DUMMY_INJECTOR_KEY)
                .config(config)
                .ioDispatcher(Dispatchers.IO)
                .build()
                .inject(this)
            return null
        }
    }

    data class Args(
        val clientSecret: String,
        val timeLimit: Duration,
        val initialDelay: Duration,
        val maxAttempts: Int,
        @InjectorKey internal val injectorKey: String = DUMMY_INJECTOR_KEY
    )
}

private fun countdownFlow(duration: Duration) = flow {
    var remainingDuration = duration
    emit(remainingDuration)

    while (remainingDuration > ZERO) {
        delay(1.seconds)
        remainingDuration -= 1.seconds
        emit(remainingDuration)
    }
}

private fun StripeIntent.Status.toPollingState(): PollingState {
    return when (this) {
        StripeIntent.Status.RequiresAction -> PollingState.Active
        StripeIntent.Status.Succeeded -> PollingState.Success
        StripeIntent.Status.Canceled,
        StripeIntent.Status.Processing,
        StripeIntent.Status.RequiresConfirmation,
        StripeIntent.Status.RequiresPaymentMethod,
        StripeIntent.Status.RequiresCapture -> PollingState.Failed
    }
}
