package com.stripe.android.paymentsheet.example.samples.ui.embedded_payment_element

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.example.samples.model.updateWithResponse
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCheckoutRequest
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCheckoutResponse
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCreateAndConfirmErrorResponse
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCreateAndConfirmIntentRequest
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCreateAndConfirmIntentResponse
import com.stripe.android.paymentsheet.example.samples.networking.awaitModel
import com.stripe.android.paymentsheet.example.samples.networking.toCheckoutRequest
import com.stripe.android.paymentsheet.example.samples.networking.toCreateIntentRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import com.github.kittinunf.result.Result as ApiResult

internal class EmbeddedPaymentElementExampleViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(EmbeddedPaymentElementExampleViewState())
    val state: StateFlow<EmbeddedPaymentElementExampleViewState> = _state

    init {
        viewModelScope.launch(Dispatchers.IO) {
            prepareCheckout()
        }
    }

    fun handleConfigured(error: Throwable?) {
        _state.update {
            it.copy(
                isProcessing = false,
                status = error?.message,
            )
        }
    }

    fun statusDisplayed() {
        _state.update { it.copy(status = null) }
    }

    suspend fun createAndConfirmIntent(
        paymentMethod: PaymentMethod,
        shouldSavePaymentMethod: Boolean,
    ): CreateIntentResult = withContext(Dispatchers.IO) {
        val request = state.value.cartState.toCreateIntentRequest(
            paymentMethodId = paymentMethod.id,
            shouldSavePaymentMethod = shouldSavePaymentMethod,
            returnUrl = "stripesdk://payment_return_url/com.stripe.android.paymentsheet.example",
        )

        val requestBody = Json.encodeToString(ExampleCreateAndConfirmIntentRequest.serializer(), request)

        val apiResult = Fuel
            .post("$BACKEND_URL/confirm_intent")
            .jsonBody(requestBody)
            .suspendable()
            .awaitModel(ExampleCreateAndConfirmIntentResponse.serializer())

        when (apiResult) {
            is ApiResult.Success -> {
                CreateIntentResult.Success(apiResult.value.clientSecret)
            }
            is ApiResult.Failure -> {
                val error = ExampleCreateAndConfirmErrorResponse.deserialize(
                    apiResult.error.response
                ).error
                CreateIntentResult.Failure(
                    cause = RuntimeException(error),
                    displayMessage = error,
                )
            }
        }
    }

    fun handleResult(result: EmbeddedPaymentElement.Result) {
        val status = when (result) {
            is EmbeddedPaymentElement.Result.Canceled -> null
            is EmbeddedPaymentElement.Result.Completed -> "Success"
            is EmbeddedPaymentElement.Result.Failed -> result.error.message
        }

        _state.update {
            it.copy(
                isProcessing = false,
                status = status,
                didComplete = result is EmbeddedPaymentElement.Result.Completed,
            )
        }
    }

    fun handleBuyButtonPressed() {
        _state.update { it.copy(isProcessing = true) }
    }

    fun retry() {
        viewModelScope.launch(Dispatchers.IO) {
            prepareCheckout()
        }
    }

    private suspend fun prepareCheckout() {
        _state.update { it.copy(isProcessing = true, isError = false) }

        val currentCartState = _state.value.cartState
        val request = currentCartState.toCheckoutRequest()
        val requestBody = Json.encodeToString(ExampleCheckoutRequest.serializer(), request)

        val apiResult = Fuel
            .post("$BACKEND_URL/checkout")
            .jsonBody(requestBody)
            .suspendable()
            .awaitModel(ExampleCheckoutResponse.serializer())

        when (apiResult) {
            is ApiResult.Success -> {
                val newCartState = currentCartState.updateWithResponse(apiResult.value)

                PaymentConfiguration.init(
                    context = getApplication(),
                    publishableKey = apiResult.value.publishableKey,
                )

                _state.update {
                    it.copy(cartState = newCartState)
                }
            }
            is ApiResult.Failure -> {
                _state.update {
                    it.copy(isProcessing = false, isError = true)
                }
            }
        }
    }

    companion object {
        const val BACKEND_URL = "https://stripe-mobile-payment-sheet-custom-deferred.stripedemos.com"
    }
}
