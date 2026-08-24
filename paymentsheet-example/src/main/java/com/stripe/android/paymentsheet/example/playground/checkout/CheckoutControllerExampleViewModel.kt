@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout

import android.app.Application
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.checkout.CheckoutController
import com.stripe.android.checkout.CheckoutController.Session
import com.stripe.android.paymentelement.CheckoutSessionPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class CheckoutControllerExampleViewModel(
    private val repository: CheckoutControllerExampleBackendRepository,
    private val savedStateHandle: SavedStateHandle,
    application: Application,
) : ViewModel() {

    private val selectedScenario = savedStateHandle.get<String>(SCENARIO_KEY)?.let(
        CheckoutControllerExampleScenario::valueOf
    )

    private val _status = MutableStateFlow<Status>(
        selectedScenario?.let(Status::Loading) ?: Status.ChooseScenario
    )
    val status: StateFlow<Status> = _status.asStateFlow()

    private val _sessionComplete = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionComplete: SharedFlow<Unit> = _sessionComplete.asSharedFlow()

    private val _confirmationResult = MutableStateFlow<ConfirmationResult?>(null)
    val confirmationResult: StateFlow<ConfirmationResult?> = _confirmationResult.asStateFlow()

    val controller = CheckoutController.Builder(
        application = application,
        savedStateHandle = savedStateHandle,
    ).resultCallback(::onConfirmationResult).build()

    init {
        selectedScenario?.let(::fetchAndConfigure)
        viewModelScope.launch {
            controller.session.collect { session ->
                updateConfiguredState { it.copy(session = session) }
            }
        }
    }

    fun start(scenario: CheckoutControllerExampleScenario) {
        if (savedStateHandle.get<String>(SCENARIO_KEY) != null) {
            return
        }

        savedStateHandle[SCENARIO_KEY] = scenario.name
        _status.value = Status.Loading(scenario)
        fetchAndConfigure(scenario)
    }

    fun clearConfirmationResult() {
        _confirmationResult.value = null
    }

    private fun onConfirmationResult(result: CheckoutController.Result) {
        Log.d(TAG, "Result: $result")
        _confirmationResult.value = when (result) {
            is CheckoutController.Result.Completed -> {
                val scenario = (_status.value as? Status.Configured)?.scenario
                if (scenario == CheckoutControllerExampleScenario.ShippingTax) {
                    ConfirmationResult.Completed(controller.session.value)
                } else {
                    _sessionComplete.tryEmit(Unit)
                    null
                }
            }
            is CheckoutController.Result.Canceled -> ConfirmationResult.Canceled
            is CheckoutController.Result.Failed -> {
                ConfirmationResult.Failed(result.error.message ?: "Confirmation failed")
            }
        }
    }

    private fun updateConfiguredState(update: (Status.Configured) -> Status.Configured) {
        val current = _status.value
        if (current is Status.Configured) {
            _status.value = update(current)
        }
    }

    private fun fetchAndConfigure(scenario: CheckoutControllerExampleScenario) {
        viewModelScope.launch {
            repository.fetchCheckoutSessionClientSecret(scenario).fold(
                onSuccess = { clientSecret ->
                    controller.configure(
                        clientSecret = clientSecret,
                    ).fold(
                        onSuccess = {
                            _status.value = Status.Configured(
                                scenario = scenario,
                                session = controller.session.value,
                            )
                        },
                        onFailure = { error ->
                            Log.e(TAG, "Failed to configure", error)
                            _status.value = Status.Error(
                                scenario = scenario,
                                message = error.message ?: "Configure failed",
                            )
                        },
                    )
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to fetch checkout session", error)
                    _status.value = Status.Error(
                        scenario = scenario,
                        message = error.message ?: "Unknown error",
                    )
                },
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        controller.destroy()
    }

    sealed interface Status {
        data object ChooseScenario : Status
        data class Loading(val scenario: CheckoutControllerExampleScenario) : Status
        data class Configured(
            val scenario: CheckoutControllerExampleScenario,
            val session: Session?,
        ) : Status
        data class Error(
            val scenario: CheckoutControllerExampleScenario,
            val message: String,
        ) : Status
    }

    sealed interface ConfirmationResult {
        data class Completed(val session: Session?) : ConfirmationResult
        data object Canceled : ConfirmationResult
        data class Failed(val message: String) : ConfirmationResult
    }

    companion object {
        private const val TAG = "CheckoutControllerExample"
        private const val SCENARIO_KEY = "checkout_controller_example_scenario"

        val factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as Application
                CheckoutControllerExampleViewModel(
                    repository = CheckoutControllerExampleBackendRepository(application),
                    savedStateHandle = createSavedStateHandle(),
                    application = application,
                )
            }
        }
    }
}
