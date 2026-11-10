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
import com.stripe.android.checkout.CheckoutElementsController
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutCustomer
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundDefinitions
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundSettings
import com.stripe.android.paymentsheet.example.playground.checkout.settings.checkoutElementsControllerConfiguration
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
internal class CheckoutElementsControllerExampleViewModel(
    private val repository: CheckoutElementsControllerExampleBackendRepository,
    private val savedStateHandle: SavedStateHandle,
    application: Application,
) : ViewModel() {
    val settings = CheckoutPlaygroundSettings.create(application)

    val controller = CheckoutElementsController.Builder(
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

    private val _confirmationMessage = MutableStateFlow<String?>(null)
    val confirmationMessage: StateFlow<String?> = _confirmationMessage.asStateFlow()

    private val _isConfirmationCompleted = MutableStateFlow(false)
    val isConfirmationCompleted: StateFlow<Boolean> = _isConfirmationCompleted.asStateFlow()

    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()

    private var activeSnapshot = settings.snapshot()
    private var configurationJob: Job? = null
    private var configurationGeneration = 0L

    val displayMandate: Boolean
        get() = !activeSnapshot[CheckoutPlaygroundDefinitions.Controller.payment.embeddedMandate]

    val isShippingAddressElementConfigured: Boolean
        get() = activeSnapshot[CheckoutPlaygroundDefinitions.Controller.shippingAddress.shouldSetConfiguration]

    init {
        if (_status.value is Status.Configured) {
            activeSnapshot.applyFeatureFlags()
        }
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
        activeSnapshot = settings.snapshot().also { it.applyFeatureFlags() }
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
        snapshot.applyCustomStripeApi()
        configurationJob?.cancel()
        val generation = ++configurationGeneration
        configurationJob = viewModelScope.launch {
            val fetchResult = repository.fetchCheckoutSession(
                settings = snapshot,
                backendUrl = snapshot[CheckoutPlaygroundDefinitions.session.backendUrl],
            )
            if (generation != configurationGeneration) return@launch

            fetchResult.fold(
                onSuccess = { response ->
                    val configurationResult = controller.configure(
                        clientSecret = response.clientSecret,
                        configuration = snapshot.checkoutElementsControllerConfiguration(),
                    )
                    if (generation != configurationGeneration) return@launch

                    when (configurationResult) {
                        is CheckoutElementsController.OperationResult.Succeeded -> {
                            if (snapshot[CheckoutPlaygroundDefinitions.session.customer] == CheckoutCustomer.New) {
                                response.customerId?.let(settings::saveReturningCustomer)
                            }
                            savedStateHandle[RUN_ACTIVE_KEY] = true
                            _status.value = Status.Configured
                        }
                        is CheckoutElementsController.OperationResult.Failed -> {
                            showError("Failed to configure", configurationResult.error)
                        }
                    }
                },
                onFailure = { error -> showError("Failed to fetch checkout session", error) },
            )
        }
    }

    private fun runOperation(
        successMessage: String,
        operation: suspend () -> CheckoutElementsController.OperationResult,
    ) {
        viewModelScope.launch {
            _operationMessage.value = when (val result = operation()) {
                is CheckoutElementsController.OperationResult.Succeeded -> successMessage
                is CheckoutElementsController.OperationResult.Failed -> result.displayMessage ?: "Operation failed"
            }
        }
    }

    private fun onConfirmationResult(result: CheckoutElementsController.Result) {
        Log.d(TAG, "Result: $result")
        _confirmationMessage.value = when (result) {
            is CheckoutElementsController.Result.Completed -> {
                _isConfirmationCompleted.value = true
                null
            }
            is CheckoutElementsController.Result.Canceled -> "Confirmation canceled"
            is CheckoutElementsController.Result.Failed -> {
                result.displayMessage?.let { "Confirmation failed: $it" } ?: "Confirmation failed"
            }
        }
    }

    private fun showError(prefix: String, error: Throwable) {
        Log.e(TAG, prefix, error)
        _status.value = Status.Error(error.message ?: prefix)
    }

    private fun clearMessages() {
        _confirmationMessage.value = null
        _isConfirmationCompleted.value = false
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
        private const val TAG = "CheckoutElementsControllerExample"
        private const val RUN_ACTIVE_KEY = "checkout_elements_controller_run_active"

        val factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as Application
                CheckoutElementsControllerExampleViewModel(
                    repository = CheckoutElementsControllerExampleBackendRepository(application),
                    savedStateHandle = createSavedStateHandle(),
                    application = application,
                )
            }
        }
    }
}

internal fun CheckoutPlaygroundSettings.Snapshot.applyCustomStripeApi() {
    ApiRequest.API_HOST_OVERRIDE = this[CheckoutPlaygroundDefinitions.session.customStripeApi]
        ?.takeIf(String::isNotBlank)
        ?.let { "https://$it" }
}
