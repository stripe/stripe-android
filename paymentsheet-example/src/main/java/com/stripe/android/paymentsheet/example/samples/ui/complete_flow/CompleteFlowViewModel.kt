package com.stripe.android.paymentsheet.example.samples.ui.complete_flow

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.google.gson.Gson
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.samples.model.CartState
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCheckoutResponse
import com.stripe.android.paymentsheet.example.samples.networking.toCheckoutRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.let
import com.github.kittinunf.result.Result as ApiResult

internal class CompleteFlowViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(
        value = CompleteFlowViewState(cartState = CartState.defaultWithHardcodedPrices),
    )
    val state: StateFlow<CompleteFlowViewState> = _state

    fun checkout() {
        viewModelScope.launch {
            val currentState = _state.updateAndGet {
                it.copy(isProcessing = true)
            }

            val request = currentState.cartState.toCheckoutRequest()
            val requestBody = Gson().toJson(request)

            val apiResult = suspendCoroutine { continuation ->
                Fuel.post("$backendUrl/checkout")
                    .jsonBody(requestBody)
                    .responseString { _, _, result ->
                        continuation.resume(result)
                    }
            }

            val error = (apiResult as? ApiResult.Failure)?.let {
                "Failed to prepare checkout\n${it.error.exception}"
            }

            val paymentInfo = (apiResult as? ApiResult.Success)?.let { result ->
                val response = Gson().fromJson(result.get(), ExampleCheckoutResponse::class.java)
                CompleteFlowViewState.PaymentInfo(
                    publishableKey = response.publishableKey,
                    clientSecret = response.paymentIntent,
                    customerConfiguration = response.makeCustomerConfig(),
                    shouldPresent = true,
                )
            }

            paymentInfo?.let {
                PaymentConfiguration.init(
                    context = getApplication(),
                    publishableKey = it.publishableKey,
                )
            }

            _state.update {
                it.copy(
                    isProcessing = false,
                    paymentInfo = paymentInfo,
                    status = error,
                )
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
