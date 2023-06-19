package com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.custom_flow

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.samples.model.CartState
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCheckoutRequest
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCheckoutResponse
import com.stripe.android.paymentsheet.example.samples.networking.awaitModel
import com.stripe.android.paymentsheet.example.samples.networking.toCheckoutRequest
import com.stripe.android.paymentsheet.model.PaymentOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import com.github.kittinunf.result.Result as ApiResult

internal class CustomFlowViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(
        value = CustomFlowViewState(cartState = CartState.defaultWithHardcodedPrices),
    )
    val state: StateFlow<CustomFlowViewState> = _state

    init {
        viewModelScope.launch(Dispatchers.IO) {
            prepareCheckout()
        }
    }

    fun statusDisplayed() {
        _state.update {
            it.copy(status = null)
        }
    }

    fun retry() {
        viewModelScope.launch(Dispatchers.IO) {
            prepareCheckout()
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
            it.copy(isProcessing = true, isError = false)
        }

        val request = currentState.cartState.toCheckoutRequest()
        val requestBody = Json.encodeToString(ExampleCheckoutRequest.serializer(), request)

        val apiResult = Fuel
            .post("$backendUrl/checkout")
            .jsonBody(requestBody)
            .suspendable()
            .awaitModel(ExampleCheckoutResponse.serializer())

        when (apiResult) {
            is ApiResult.Success -> {
                PaymentConfiguration.init(
                    context = getApplication(),
                    publishableKey = apiResult.value.publishableKey,
                )

                val paymentInfo = CustomFlowViewState.PaymentInfo(
                    clientSecret = apiResult.value.paymentIntent,
                    customerConfiguration = apiResult.value.makeCustomerConfig(),
                )

                _state.update {
                    it.copy(paymentInfo = paymentInfo)
                }
            }
            is ApiResult.Failure -> {
                _state.update {
                    it.copy(isProcessing = false, isError = false)
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
