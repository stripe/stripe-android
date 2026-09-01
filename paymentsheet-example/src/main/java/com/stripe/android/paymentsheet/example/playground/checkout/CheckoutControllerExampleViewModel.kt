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
import com.stripe.android.paymentsheet.example.playground.checkout.CheckoutControllerExampleSettings.Snapshot
import com.stripe.android.paymentsheet.example.playground.model.CheckoutResponse
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
    private val application: Application,
) : ViewModel() {

    private val customerStore = CheckoutControllerExampleCustomerStore(application)
    private val _settings = MutableStateFlow(
        CheckoutControllerExampleSettings.create(
            persistedValues = savedStateHandle.get<Map<String, String>>(SETTINGS_KEY),
            storedCustomerId = customerStore.getCustomerId(),
        )
    )
    val settings: StateFlow<CheckoutControllerExampleSettings> = _settings.asStateFlow()

    private val restoredSessionSettings = if (savedStateHandle.get<Boolean>(SESSION_STARTED_KEY) == true) {
        settings.value.snapshot()
    } else {
        null
    }

    private val _status = MutableStateFlow<Status>(
        restoredSessionSettings?.let(Status::Loading) ?: Status.ChooseSettings
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
        restoredSessionSettings?.let(::fetchAndConfigure)
        viewModelScope.launch {
            controller.session.collect { session ->
                updateConfiguredState { it.copy(session = session) }
            }
        }
    }

    fun updateSetting(
        definition: CheckoutControllerExampleSettingDefinition<Any>,
        value: Any,
    ) {
        updateSettings(_settings.value.withValue(definition, value))
    }

    fun start() {
        if (savedStateHandle.get<Boolean>(SESSION_STARTED_KEY) == true) {
            return
        }

        val sessionSettings = settings.value.snapshot()
        persistSettings(settings.value)
        savedStateHandle[SESSION_STARTED_KEY] = true
        _status.value = Status.Loading(sessionSettings)
        fetchAndConfigure(sessionSettings)
    }

    fun clearConfirmationResult() {
        _confirmationResult.value = null
    }

    private fun onConfirmationResult(result: CheckoutController.Result) {
        Log.d(TAG, "Result: $result")
        _confirmationResult.value = when (result) {
            is CheckoutController.Result.Completed -> {
                _sessionComplete.tryEmit(Unit)
                null
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

    private fun updateSettings(updatedSettings: CheckoutControllerExampleSettings) {
        _settings.value = updatedSettings
        persistSettings(updatedSettings)
    }

    private fun persistSettings(settings: CheckoutControllerExampleSettings) {
        savedStateHandle[SETTINGS_KEY] = settings.encodedValues()
    }

    private fun fetchAndConfigure(settings: Snapshot) {
        viewModelScope.launch {
            val result = runCatching {
                repository.fetchCheckoutSession(settings)
            }.getOrElse { error -> kotlin.Result.failure(error) }
            val response = result.getOrNull()
            if (response == null) {
                val error = result.exceptionOrNull()
                Log.e(TAG, "Failed to fetch checkout session", error)
                _status.value = Status.Error(
                    settings = settings,
                    message = error?.message ?: "Unknown error",
                )
                return@launch
            }
            configure(settings, response)
        }
    }

    private suspend fun configure(
        settings: Snapshot,
        response: CheckoutResponse,
    ) {
        if (settings[CheckoutControllerExampleSettingsDefinition.Customer] == CheckoutControllerExampleCustomer.New) {
            val customerId = response.customerId?.takeIf(String::isNotBlank)
            if (customerId == null) {
                _status.value = Status.Error(
                    settings = settings,
                    message = "No customer ID returned for new customer",
                )
                return
            }
            customerStore.saveCustomerId(customerId)
            updateSettings(
                _settings.value
                    .withStoredCustomerId(customerId)
                    .withValue(
                        CheckoutControllerExampleSettingsDefinition.Customer,
                        CheckoutControllerExampleCustomer.Existing(customerId),
                    )
            )
        }

        controller.configure(
            clientSecret = response.clientSecret,
        ).fold(
            onSuccess = {
                _status.value = Status.Configured(
                    settings = settings,
                    session = controller.session.value,
                )
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to configure", error)
                _status.value = Status.Error(
                    settings = settings,
                    message = error.message ?: "Configure failed",
                )
            },
        )
    }

    override fun onCleared() {
        super.onCleared()
        controller.destroy()
    }

    sealed interface Status {
        data object ChooseSettings : Status
        data class Loading(val settings: Snapshot) : Status
        data class Configured(
            val settings: Snapshot,
            val session: Session?,
        ) : Status
        data class Error(
            val settings: Snapshot,
            val message: String,
        ) : Status
    }

    sealed interface ConfirmationResult {
        data object Canceled : ConfirmationResult
        data class Failed(val message: String) : ConfirmationResult
    }

    companion object {
        private const val TAG = "CheckoutControllerExample"
        private const val SETTINGS_KEY = "checkout_controller_example_settings"
        private const val SESSION_STARTED_KEY = "checkout_controller_example_session_started"

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
