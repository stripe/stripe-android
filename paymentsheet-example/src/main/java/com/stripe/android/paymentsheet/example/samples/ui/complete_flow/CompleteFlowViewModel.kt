package com.stripe.android.paymentsheet.example.samples.ui.complete_flow

import androidx.lifecycle.ViewModel
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.google.gson.Gson
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.samples.model.CartState
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCheckoutResponse
import com.stripe.android.paymentsheet.example.samples.networking.toCheckoutRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlin.Result
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.let
import com.github.kittinunf.result.Result as ApiResult

internal class CompleteFlowViewModel : ViewModel() {

    private val _state = MutableStateFlow(
        value = CompleteFlowViewState(cartState = CartState.static),
    )
    val state: StateFlow<CompleteFlowViewState> = _state

    suspend fun prepareCheckout(): PrepareCheckoutResult? {
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

        val error = (result as? ApiResult.Failure)?.let {
            "Failed to prepare checkout\n${it.error.exception}"
        }

        _state.update {
            it.copy(isProcessing = false, status = error)
        }

        return when (result) {
            is ApiResult.Success -> {
                val response = Gson().fromJson(result.get(), ExampleCheckoutResponse::class.java)
                PrepareCheckoutResult(
                    customerConfig = response.makeCustomerConfig(),
                    clientSecret = response.paymentIntent,
                    publishableKey = response.publishableKey,
                )
            }
            is ApiResult.Failure -> {
                null
            }
        }
    }

    fun statusDisplayed() {
        _state.update {
            it.copy(status = null)
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

    data class PrepareCheckoutResult(
        val customerConfig: PaymentSheet.CustomerConfiguration?,
        val clientSecret: String,
        val publishableKey: String,
    )

    private companion object {
        const val backendUrl = "https://stripe-mobile-payment-sheet.glitch.me"
    }
}
