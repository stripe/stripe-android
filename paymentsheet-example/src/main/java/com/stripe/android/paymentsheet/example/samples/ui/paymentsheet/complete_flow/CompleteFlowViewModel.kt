package com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.complete_flow

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.result.Result
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.samples.model.CartState
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCheckoutRequest
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCheckoutResponse
import com.stripe.android.paymentsheet.example.samples.networking.awaitModel
import com.stripe.android.paymentsheet.example.samples.networking.toCheckoutRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

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
            val requestBody = Json.encodeToString(ExampleCheckoutRequest.serializer(), request)

            val apiResult = Fuel
                .post("$backendUrl/checkout")
                .jsonBody(requestBody)
                .suspendable()
                .awaitModel(ExampleCheckoutResponse.serializer())

            when (apiResult) {
                is Result.Success -> {
                    PaymentConfiguration.init(
                        context = getApplication(),
                        publishableKey = apiResult.value.publishableKey,
                    )
                    val paymentInfo = CompleteFlowViewState.PaymentInfo(
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
            it.copy(
                isProcessing = false,
                status = status,
                didComplete = paymentResult is PaymentSheetResult.Completed,
            )
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
