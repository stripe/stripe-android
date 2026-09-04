@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

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
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutCustomer
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundDefinitions
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundSettings
import com.stripe.android.paymentsheet.example.playground.checkout.settings.checkoutControllerConfiguration
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
internal class CheckoutControllerExampleViewModel(
    private val repository: CheckoutControllerExampleBackendRepository,
    private val savedStateHandle: SavedStateHandle,
    application: Application,
) : ViewModel() {
    val settings = CheckoutPlaygroundSettings.create(application)

    val controller = CheckoutController.Builder(
        application = application,
        savedStateHandle = savedStateHandle,
    ).resultCallback(::onConfirmationResult).build()

    private val _status = MutableStateFlow<Status>(
        if (savedStateHandle.get<Boolean>(RUN_ACTIVE_KEY) == true && controller.session.value != null) {
            Status.Configured
        } else {
            Status.Settings
        }
    )
    val status: StateFlow<Status> = _status.asStateFlow()

    private val _sessionComplete = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionComplete: SharedFlow<Unit> = _sessionComplete.asSharedFlow()

    private val _confirmationMessage = MutableStateFlow<String?>(null)
    val confirmationMessage: StateFlow<String?> = _confirmationMessage.asStateFlow()

    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()

    private var activeSnapshot = settings.snapshot()
    private var configurationJob: Job? = null
    private var configurationGeneration = 0L

    val displayMandate: Boolean
        get() = !activeSnapshot[CheckoutPlaygroundDefinitions.Controller.payment.embeddedMandate]

    init {
        savedStateHandle[RUN_ACTIVE_KEY] = _status.value is Status.Configured
    }

    fun returnToSettings() {
        configurationGeneration++
        configurationJob?.cancel()
        configurationJob = null
        savedStateHandle[RUN_ACTIVE_KEY] = false
        _status.value = Status.Settings
        clearMessages()
    }

    fun start() {
        if (settings.validationErrors().isNotEmpty()) return
        activeSnapshot = settings.snapshot()
        savedStateHandle[RUN_ACTIVE_KEY] = false
        clearMessages()
        _status.value = Status.Loading
        configure(activeSnapshot)
    }

    fun retry() {
        savedStateHandle[RUN_ACTIVE_KEY] = false
        clearMessages()
        _status.value = Status.Loading
        configure(activeSnapshot)
    }

    fun clearConfirmationMessage() {
        _confirmationMessage.value = null
    }

    fun clearPaymentOption() {
        runOperation("Payment option cleared") { controller.clearPaymentOption() }
    }

    fun applyPromotionCode(code: String) {
        runOperation("Promotion code applied") { controller.applyPromotionCode(code) }
    }

    fun removePromotionCode() {
        runOperation("Promotion code removed") { controller.removePromotionCode() }
    }

    fun updateEmail(email: String) {
        runOperation("Email updated") { controller.updateEmail(email.trim().ifEmpty { null }) }
    }

    private fun configure(snapshot: CheckoutPlaygroundSettings.Snapshot) {
        configurationJob?.cancel()
        val generation = ++configurationGeneration
        configurationJob = viewModelScope.launch {
            val request = CheckoutControllerExampleRequestFactory.create(settings = snapshot)
            val fetchResult = repository.fetchCheckoutSession(
                request = request,
                backendUrl = snapshot[CheckoutPlaygroundDefinitions.session.backendUrl],
            )
            if (generation != configurationGeneration) return@launch

            fetchResult.fold(
                onSuccess = { response ->
                    val configurationResult = controller.configure(
                        clientSecret = response.clientSecret,
                        configuration = snapshot.checkoutControllerConfiguration(),
                    )
                    if (generation != configurationGeneration) return@launch

                    configurationResult.fold(
                        onSuccess = {
                            if (snapshot[CheckoutPlaygroundDefinitions.session.customer] == CheckoutCustomer.New) {
                                response.customerId?.let(settings::saveReturningCustomer)
                            }
                            savedStateHandle[RUN_ACTIVE_KEY] = true
                            _status.value = Status.Configured
                        },
                        onFailure = { error -> showError("Failed to configure", error) },
                    )
                },
                onFailure = { error -> showError("Failed to fetch checkout session", error) },
            )
        }
    }

    private fun runOperation(
        successMessage: String,
        operation: suspend () -> Result<Unit>,
    ) {
        viewModelScope.launch {
            _operationMessage.value = operation().fold(
                onSuccess = { successMessage },
                onFailure = { it.message ?: "Operation failed" },
            )
        }
    }

    private fun onConfirmationResult(result: CheckoutController.Result) {
        Log.d(TAG, "Result: $result")
        _confirmationMessage.value = when (result) {
            is CheckoutController.Result.Completed -> {
                _sessionComplete.tryEmit(Unit)
                null
            }
            is CheckoutController.Result.Canceled -> "Confirmation canceled"
            is CheckoutController.Result.Failed -> {
                result.error.message?.let { "Confirmation failed: $it" } ?: "Confirmation failed"
            }
        }
    }

    private fun showError(prefix: String, error: Throwable) {
        Log.e(TAG, prefix, error)
        _status.value = Status.Error(error.message ?: prefix)
    }

    private fun clearMessages() {
        _confirmationMessage.value = null
        _operationMessage.value = null
    }

    override fun onCleared() {
        controller.destroy()
        super.onCleared()
    }

    sealed interface Status {
        data object Settings : Status
        data object Loading : Status
        data object Configured : Status
        data class Error(val message: String) : Status
    }

    companion object {
        private const val TAG = "CheckoutControllerExample"
        private const val RUN_ACTIVE_KEY = "checkout_controller_run_active"

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
