package com.stripe.android.paymentsheet.example.samples.ui.server_side_confirm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.google.gson.Gson
import com.stripe.android.CreateIntentCallback
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.example.samples.model.CartProduct
import com.stripe.android.paymentsheet.example.samples.model.CartState
import com.stripe.android.paymentsheet.example.samples.model.updateWithResponse
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCheckoutResponse
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCreateAndConfirmIntentResponse
import com.stripe.android.paymentsheet.example.samples.networking.ExampleUpdateResponse
import com.stripe.android.paymentsheet.example.samples.networking.awaitModel
import com.stripe.android.paymentsheet.example.samples.networking.toCheckoutRequest
import com.stripe.android.paymentsheet.example.samples.networking.toCreateIntentRequest
import com.stripe.android.paymentsheet.example.samples.networking.toUpdateRequest
import com.stripe.android.paymentsheet.model.PaymentOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.Result
import kotlin.time.Duration.Companion.milliseconds
import com.github.kittinunf.result.Result as ApiResult

internal class ServerSideConfirmationViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(
        value = ServerSideConfirmationViewState(confirmedCartState = CartState.default),
    )
    val state: StateFlow<ServerSideConfirmationViewState> = _state

    init {
        viewModelScope.launch(Dispatchers.IO) {
            prepareCheckout()
        }

        viewModelScope.launch {
            observeCartUpdates()
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
                requiresFlowControllerConfigure = false,
                status = error?.let { "Failed to configure\n$it" },
            )
        }
    }

    fun handlePaymentOptionChanged(paymentOption: PaymentOption?) {
        viewModelScope.launch {
            _state.update {
                it.copy(paymentOption = paymentOption)
            }
        }
    }

    suspend fun createAndConfirmIntent(
        paymentMethodId: String,
        shouldSavePaymentMethod: Boolean,
    ): CreateIntentCallback.Result = withContext(Dispatchers.IO) {
        val request = state.value.cartState.toCreateIntentRequest(
            paymentMethodId = paymentMethodId,
            shouldSavePaymentMethod = shouldSavePaymentMethod,
            returnUrl = "stripesdk://payment_return_url/com.stripe.android.paymentsheet.example",
        )

        val requestBody = Gson().toJson(request)

        val apiResult = Fuel
            .post("$backendUrl/confirm_intent")
            .jsonBody(requestBody)
            .suspendable()
            .awaitModel<ExampleCreateAndConfirmIntentResponse>()

        when (apiResult) {
            is ApiResult.Success -> {
                CreateIntentCallback.Result.Success(apiResult.value.clientSecret)
            }
            is ApiResult.Failure -> {
                val errorMessage = "Unable to create intent\n${apiResult.error.exception}"
                CreateIntentCallback.Result.Failure(
                    cause = RuntimeException(errorMessage),
                    displayMessage = "Something went wrong…",
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

    fun updateQuantity(productId: CartProduct.Id, newQuantity: Int) {
        val dirtyCartState = state.value.cartState.updateQuantity(productId, newQuantity)
        _state.update {
            it.copy(dirtyCartState = dirtyCartState)
        }
    }

    fun updateSubscription(isSubscription: Boolean) {
        val dirtyCartState = state.value.cartState.copy(
            isSubscription = isSubscription,
            requiresCalculation = true,
        )
        _state.update {
            it.copy(dirtyCartState = dirtyCartState)
        }
    }

    fun handleBuyButtonPressed() {
        _state.update {
            it.copy(isProcessing = true)
        }
    }

    private suspend fun prepareCheckout() {
        val currentState = _state.updateAndGet {
            it.copy(isProcessing = true)
        }

        val request = currentState.cartState.toCheckoutRequest()
        val requestBody = Gson().toJson(request)

        val apiResult = Fuel
            .post("$backendUrl/checkout")
            .jsonBody(requestBody)
            .suspendable()
            .awaitModel<ExampleCheckoutResponse>()

        when (apiResult) {
            is ApiResult.Success -> {
                val newCartState = currentState.cartState.updateWithResponse(apiResult.value)

                PaymentConfiguration.init(
                    context = getApplication(),
                    publishableKey = apiResult.value.publishableKey,
                )

                _state.update {
                    it.copy(
                        isProcessing = false,
                        isError = false,
                        confirmedCartState = newCartState,
                        dirtyCartState = null,
                        requiresFlowControllerConfigure = true,
                    )
                }
            }
            is ApiResult.Failure -> {
                val status = "Preparing checkout failed\n${apiResult.error.message}"
                _state.update {
                    it.copy(isProcessing = false, isError = true, status = status)
                }
            }
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun observeCartUpdates() {
        state
            .mapNotNull { it.dirtyCartState }
            .debounce(300.milliseconds)
            .map(::updateCart)
            .collect(::handleCartUpdated)
    }

    private fun handleCartUpdated(result: Result<CartState>) {
        val currentState = state.value

        val newCartState = result.getOrNull() ?: currentState.confirmedCartState
        val status = result.exceptionOrNull()?.let { "Failed to update cart\n$it" }

        _state.update {
            it.copy(
                // Keep processing on success, because we'll re-configure FlowController
                isProcessing = result.isSuccess,
                confirmedCartState = newCartState,
                dirtyCartState = null,
                status = status,
                requiresFlowControllerConfigure = result.isSuccess,
            )
        }
    }

    private suspend fun updateCart(
        currentState: CartState,
    ): Result<CartState> = withContext(Dispatchers.IO) {
        _state.update { it.copy(isProcessing = true) }

        val request = currentState.toUpdateRequest()
        val requestBody = Gson().toJson(request)

        val apiResult = Fuel
            .post("$backendUrl/compute_totals")
            .jsonBody(requestBody)
            .suspendable()
            .awaitModel<ExampleUpdateResponse>()

        when (apiResult) {
            is ApiResult.Success -> {
                val newCartState = currentState.updateWithResponse(apiResult.value)
                Result.success(newCartState)
            }
            is ApiResult.Failure -> {
                Result.failure(apiResult.error.exception)
            }
        }
    }

    companion object {
        const val backendUrl = "https://stripe-mobile-payment-sheet-custom-deferred.glitch.me"
    }
}
