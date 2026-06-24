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
import com.stripe.android.checkout.CheckoutSession
import com.stripe.android.checkout.PaymentElement
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

    val controller = CheckoutController(
        application = application,
        savedStateHandle = savedStateHandle,
        resultCallback = { result ->
            Log.d(TAG, "Result: $result")
        },
    )

    init {
        viewModelScope.launch {
            fetchAndConfigure()
        }
        viewModelScope.launch {
            controller.checkoutSession.collect { session ->
                updateConfiguredState { it.copy(checkoutSession = session) }
            }
        }
        viewModelScope.launch {
            controller.isLoading.collect { loading ->
                updateConfiguredState { it.copy(isLoading = loading) }
            }
        }
        viewModelScope.launch {
            controller.paymentOption.collect { option ->
                updateConfiguredState { it.copy(paymentOption = option) }
            }
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
                controller.configure(clientSecret).fold(
                    onSuccess = {
                        _status.value = Status.Configured(
                            checkoutSession = controller.checkoutSession.value,
                            isLoading = controller.isLoading.value,
                            paymentOption = controller.paymentOption.value,
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
            val checkoutSession: CheckoutSession?,
            val isLoading: Boolean,
            val paymentOption: PaymentElement.PaymentOptionDisplayData?,
        ) : Status
        data class Error(val message: String) : Status
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
