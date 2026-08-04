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
import com.stripe.android.checkout.GooglePayConfiguration
import com.stripe.android.checkout.GooglePayConfiguration.Environment
import com.stripe.android.paymentelement.CheckoutSessionPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class CheckoutControllerExampleViewModel(
    private val repository: CheckoutControllerExampleBackendRepository,
    savedStateHandle: SavedStateHandle,
    application: Application,
) : ViewModel() {

    private val _status = MutableStateFlow<Status>(Status.Loading)
    val status: StateFlow<Status> = _status.asStateFlow()

    private val _confirmationResult = MutableStateFlow<ConfirmationResult?>(null)
    val confirmationResult: StateFlow<ConfirmationResult?> = _confirmationResult.asStateFlow()

    val controller = CheckoutController.Builder(
        application = application,
        savedStateHandle = savedStateHandle,
    ).resultCallback { result ->
        when (result) {
            is CheckoutController.Result.Completed -> {
                _confirmationResult.value = ConfirmationResult.Completed
            }
            is CheckoutController.Result.Failed -> {
                _confirmationResult.value = ConfirmationResult.Failed(
                    result.error.message ?: "An unknown error occurred."
                )
            }
            is CheckoutController.Result.Canceled -> Unit
        }
    }.build()

    init {
        viewModelScope.launch {
            fetchAndConfigure()
        }
        viewModelScope.launch {
            controller.session.collect { session ->
                updateConfiguredState { it.copy(session = session) }
            }
        }
    }

    fun startNewPayment() {
        _confirmationResult.value = null
        _status.value = Status.Loading
        viewModelScope.launch {
            fetchAndConfigure()
        }
    }

    private fun updateConfiguredState(update: (Status.Configured) -> Status.Configured) {
        val current = _status.value
        if (current is Status.Configured) {
            _status.value = update(current)
        }
    }

    private suspend fun fetchAndConfigure() {
        repository.fetchCheckoutSessionClientSecret().fold(
            onSuccess = { clientSecret ->
                controller.configure(
                    checkoutSessionClientSecret = clientSecret,
                    configuration = CheckoutController.Configuration()
                        .googlePayConfiguration(
                            GooglePayConfiguration(
                                environment = Environment.Test
                            )
                        )
                ).fold(
                    onSuccess = {
                        _status.value = Status.Configured(
                            session = controller.session.value,
                        )
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to configure", error)
                        _status.value = Status.Error(error.message ?: "Configure failed")
                    },
                )
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to fetch checkout session", error)
                _status.value = Status.Error(error.message ?: "Unknown error")
            },
        )
    }

    override fun onCleared() {
        super.onCleared()
        controller.destroy()
    }

    sealed interface Status {
        data object Loading : Status
        data class Configured(
            val session: Session?,
        ) : Status
        data class Error(val message: String) : Status
    }

    sealed interface ConfirmationResult {
        data object Completed : ConfirmationResult
        data class Failed(val message: String) : ConfirmationResult
    }

    companion object {
        private const val TAG = "CheckoutControllerExample"

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
