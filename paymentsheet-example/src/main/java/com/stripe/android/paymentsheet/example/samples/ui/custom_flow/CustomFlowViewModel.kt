package com.stripe.android.paymentsheet.example.samples.ui.custom_flow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.google.gson.Gson
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.samples.model.CartState
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCheckoutResponse
import com.stripe.android.paymentsheet.example.samples.networking.toCheckoutRequest
import com.stripe.android.paymentsheet.model.PaymentOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import com.github.kittinunf.result.Result as ApiResult

internal class CustomFlowViewModel : ViewModel() {

    private val _state = MutableStateFlow(
        value = CustomFlowViewState(cartState = CartState.defaultWithHardcodedPrices),
    )
    val state: StateFlow<CustomFlowViewState> = _state

    init {
        viewModelScope.launch {
            prepareCheckout()
        }
    }

    fun statusDisplayed() {
        _state.update {
            it.copy(status = null)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun handleFlowControllerConfigured(success: Boolean, error: Throwable?) {
        _state.update {
            it.copy(
                isProcessing = false,
                status = error?.let { e -> "Failed to configure\n$e" },
            )
        }
    }

    fun handlePaymentOptionChanged(paymentOption: PaymentOption?) {
        viewModelScope.launch {
            _state.update {
                it.copy(paymentOption = paymentOption,)
            }
        }
    }

    fun handlePaymentSheetResult(paymentResult: PaymentSheetResult) {
        val status = when (paymentResult) {
            is PaymentSheetResult.Canceled -> null
            is PaymentSheetResult.Completed -> "Success"
            is PaymentSheetResult.Failed -> paymentResult.error.message
        }

        _state.update {
            it.copy(
                isProcessing = false,
                status = status,
                didComplete = paymentResult is PaymentSheetResult.Completed,
            )
        }
    }

    private suspend fun prepareCheckout() {
        val currentState = _state.updateAndGet {
            it.copy(isProcessing = true)
        }

        val request = currentState.cartState.toCheckoutRequest()
        val requestBody = Gson().toJson(request)

        val result = suspendCoroutine { continuation ->
            Fuel.post("$backendUrl/checkout")
                .jsonBody(requestBody)
                .responseString { _, _, result ->
                    continuation.resume(result)
                }
        }

        when (result) {
            is ApiResult.Success -> {
                val response = Gson().fromJson(result.get(), ExampleCheckoutResponse::class.java)
                val paymentInfo = CustomFlowViewState.PaymentInfo(
                    publishableKey = response.publishableKey,
                    clientSecret = response.paymentIntent,
                    customerConfiguration = response.makeCustomerConfig(),
                )

                _state.update {
                    it.copy(paymentInfo = paymentInfo)
                }
            }
            is ApiResult.Failure -> {
                val status = "Preparing checkout failed\n${result.error.message}"
                _state.update {
                    it.copy(isProcessing = false, status = status)
                }
            }
        }
    }

    fun handleBuyButtonPressed() {
        _state.update {
            it.copy(isProcessing = true)
        }
    }

    companion object {
        const val backendUrl = "https://stripe-mobile-payment-sheet.glitch.me"
    }
}
