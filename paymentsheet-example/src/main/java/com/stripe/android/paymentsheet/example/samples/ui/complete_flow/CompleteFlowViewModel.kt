package com.stripe.android.paymentsheet.example.samples.ui.complete_flow

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.awaitResult
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.samples.model.CartState
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCheckoutResponse
import com.stripe.android.paymentsheet.example.samples.networking.toCheckoutRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

internal class CompleteFlowViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(
        value = CompleteFlowViewState(cartState = CartState.defaultWithHardcodedPrices),
    )
    val state: StateFlow<CompleteFlowViewState> = _state

    fun checkout() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentState = _state.updateAndGet {
                it.copy(isProcessing = true)
            }

            val request = currentState.cartState.toCheckoutRequest()
            val requestBody = Gson().toJson(request)

            val apiResult = Fuel
                .post("$backendUrl/checkout")
                .jsonBody(requestBody)
                .suspendable()
                .awaitResult(ExampleCheckoutResponse.Deserializer)

            when (apiResult) {
                is Result.Success -> {
                    val paymentInfo = CompleteFlowViewState.PaymentInfo(
                        publishableKey = apiResult.value.publishableKey,
                        clientSecret = apiResult.value.paymentIntent,
                        customerConfiguration = apiResult.value.makeCustomerConfig(),
                        shouldPresent = true,
                    )

                    _state.update {
                        it.copy(
                            isProcessing = false,
                            paymentInfo = paymentInfo,
                            status = null,
                        )
                    }
                }
                is Result.Failure -> {
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            paymentInfo = null,
                            status = "Failed to prepare checkout\n${apiResult.error.exception}",
                        )
                    }
                }
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
            it.copy(status = status)
        }
    }

    fun statusDisplayed() {
        _state.update {
            it.copy(status = null)
        }
    }

    fun paymentSheetPresented() {
        _state.update {
            it.copy(
                paymentInfo = it.paymentInfo?.copy(shouldPresent = false)
            )
        }
    }

    private companion object {
        const val backendUrl = "https://stripe-mobile-payment-sheet.glitch.me"
    }
}
