package com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.server_side_confirm.custom_flow

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
import com.stripe.android.paymentsheet.example.samples.model.CartProduct
import com.stripe.android.paymentsheet.example.samples.model.CartState
import com.stripe.android.paymentsheet.example.samples.model.updateWithResponse
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCheckoutRequest
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCheckoutResponse
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCreateAndConfirmErrorResponse
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCreateAndConfirmIntentRequest
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCreateAndConfirmIntentResponse
import com.stripe.android.paymentsheet.example.samples.networking.ExampleUpdateRequest
import com.stripe.android.paymentsheet.example.samples.networking.ExampleUpdateResponse
import com.stripe.android.paymentsheet.example.samples.networking.awaitModel
import com.stripe.android.paymentsheet.example.samples.networking.toCheckoutRequest
import com.stripe.android.paymentsheet.example.samples.networking.toCreateIntentRequest
import com.stripe.android.paymentsheet.example.samples.networking.toUpdateRequest
import com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.server_side_confirm.custom_flow.ServerSideConfirmationCustomFlowViewModel.ConfigureResult
import com.stripe.android.paymentsheet.model.PaymentOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.Result
import kotlin.time.Duration.Companion.milliseconds
import com.github.kittinunf.result.Result as ApiResult

private typealias ConfigureHandler = suspend (CartState) -> ConfigureResult

internal class ServerSideConfirmationCustomFlowViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(
        value = ServerSideConfirmationCustomFlowViewState(confirmedCartState = CartState.default),
    )
    val state: StateFlow<ServerSideConfirmationCustomFlowViewState> = _state

    private var configureHandler: ConfigureHandler? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            prepareCheckout()
        }

        viewModelScope.launch {
            observeCartUpdates()
        }
    }

    fun registerFlowControllerConfigureHandler(handler: ConfigureHandler) {
        this.configureHandler = handler
    }

    fun unregisterFlowControllerConfigureHandler() {
        this.configureHandler = null
    }

    fun statusDisplayed() {
        _state.update {
            it.copy(status = null)
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
        paymentMethod: PaymentMethod,
        shouldSavePaymentMethod: Boolean,
    ): CreateIntentResult = withContext(Dispatchers.IO) {
        val request = state.value.cartState.toCreateIntentRequest(
            paymentMethodId = paymentMethod.id!!,
            shouldSavePaymentMethod = shouldSavePaymentMethod,
            returnUrl = "stripesdk://payment_return_url/com.stripe.android.paymentsheet.example",
        )

        val requestSerializer = ExampleCreateAndConfirmIntentRequest.serializer()
        val requestBody = Json.encodeToString(requestSerializer, request)

        val apiResult = Fuel
            .post("$backendUrl/confirm_intent")
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

    fun updateQuantity(productId: CartProduct.Id, newQuantity: Int) {
        val dirtyCartState = state.value.cartState.updateQuantity(productId, newQuantity)
        _state.update {
            it.copy(
                dirtyCartState = dirtyCartState
            )
        }
    }

    fun updateSubscription(isSubscription: Boolean) {
        val dirtyCartState = state.value.cartState.copy(
            isSubscription = isSubscription,
        )
        _state.update {
            it.copy(
                dirtyCartState = dirtyCartState,
            )
        }
    }

    fun handleBuyButtonPressed() {
        _state.update {
            it.copy(isProcessing = true)
        }
    }

    fun retry() {
        viewModelScope.launch(Dispatchers.IO) {
            prepareCheckout()
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
                val newCartState = currentState.cartState.updateWithResponse(apiResult.value)

                PaymentConfiguration.init(
                    context = getApplication(),
                    publishableKey = apiResult.value.publishableKey,
                )

                _state.update {
                    it.copy(
                        dirtyCartState = newCartState,
                    )
                }
            }
            is ApiResult.Failure -> {
                _state.update {
                    it.copy(isProcessing = false, isError = true)
                }
            }
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun observeCartUpdates() {
        state
            .mapNotNull { it.dirtyCartState }
            .distinctUntilChanged()
            .debounce(300.milliseconds)
            .onEach { renderProcessing() }
            .map(::updateCart)
            .collect(::handleCartUpdated)
    }

    private suspend fun configureFlowControllerWithNewCart(
        cartState: CartState,
    ): Result<CartState> {
        return configureHandler?.let { configure ->
            val result = configure(cartState)

            _state.update {
                it.copy(paymentOption = result.paymentOption)
            }

            if (result.error != null) {
                Result.failure(result.error)
            } else {
                Result.success(cartState)
            }
        } ?: Result.failure(IllegalStateException("Failed to update cart"))
    }

    private suspend fun handleCartUpdated(result: Result<CartState>) {
        val cartUpdateResult = result.fold(
            onSuccess = { updatedCartState ->
                configureFlowControllerWithNewCart(updatedCartState)
            },
            onFailure = { error ->
                Result.failure(error)
            },
        )

        cartUpdateResult.fold(
            onSuccess = { finalCartState ->
                _state.update {
                    it.copy(
                        isProcessing = false,
                        confirmedCartState = finalCartState,
                        dirtyCartState = null,
                    )
                }
            },
            onFailure = { error ->
                _state.update {
                    it.copy(
                        isProcessing = false,
                        dirtyCartState = null,
                        status = "Failed to update cart\n$error",
                    )
                }
            }
        )
    }

    private fun renderProcessing() {
        _state.update {
            it.copy(isProcessing = true, isError = false)
        }
    }

    private suspend fun updateCart(
        currentState: CartState,
    ): Result<CartState> = withContext(Dispatchers.IO) {
        val request = currentState.toUpdateRequest()
        val requestBody = Json.encodeToString(ExampleUpdateRequest.serializer(), request)

        val apiResult = Fuel
            .post("$backendUrl/compute_totals")
            .jsonBody(requestBody)
            .suspendable()
            .awaitModel(ExampleUpdateResponse.serializer())

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

    data class ConfigureResult(
        val paymentOption: PaymentOption?,
        val error: Throwable?,
    )

    companion object {
        const val backendUrl = "https://stripe-mobile-payment-sheet-custom-deferred.stripedemos.com"
    }
}
