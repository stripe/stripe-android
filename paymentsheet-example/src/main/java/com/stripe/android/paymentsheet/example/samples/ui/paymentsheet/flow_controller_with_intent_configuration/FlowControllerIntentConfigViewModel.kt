package com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.flow_controller_with_intent_configuration

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.samples.model.updateWithResponse
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCheckoutRequest
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCheckoutResponse
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCreateAndConfirmErrorResponse
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCreateAndConfirmIntentRequest
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCreateAndConfirmIntentResponse
import com.stripe.android.paymentsheet.example.samples.networking.awaitModel
import com.stripe.android.paymentsheet.example.samples.networking.toCheckoutRequest
import com.stripe.android.paymentsheet.example.samples.networking.toCreateIntentRequest
import com.stripe.android.paymentsheet.model.PaymentOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import com.github.kittinunf.result.Result as ApiResult

internal class FlowControllerIntentConfigViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(FlowControllerIntentConfigViewState())
    val state: StateFlow<FlowControllerIntentConfigViewState> = _state

    init {
        viewModelScope.launch(Dispatchers.IO) {
            prepareCheckout()
        }
    }

    fun statusDisplayed() {
        _state.update { it.copy(status = null) }
    }

    fun handlePaymentOptionChanged(paymentOption: PaymentOption?) {
        _state.update { it.copy(paymentOption = paymentOption) }
    }

    fun handleFlowControllerConfigured(success: Boolean, error: Throwable?) {
        _state.update {
            it.copy(
                isProcessing = false,
                status = if (!success) error?.message else null,
            )
        }
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
