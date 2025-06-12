package com.stripe.android.paymentsheet.example.samples.ui.paymentsheet.custom_flow

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentMethodInitParamsHandler
import com.stripe.android.paymentsheet.PaymentRequestShippingContactUpdateHandler
import com.stripe.android.paymentsheet.PaymentRequestShippingRateUpdateHandler
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.WalletConfiguration
import com.stripe.android.paymentsheet.WalletConfiguration.SelectedPartialAddress
import com.stripe.android.paymentsheet.WalletConfiguration.SelectedShippingRate
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import com.github.kittinunf.result.Result as ApiResult

@Serializable
data class FetchedItem(
    val id: String,
    val name: String,
    val amount: Int
)

@Serializable
data class ShippingRateResponseItem(
    val id: String,
    val displayName: String,
    val amount: Int,
    val deliveryEstimate: String
)

@Serializable
data class ItemsResponse(
    val items: List<FetchedItem>,
    val total: Int,
    val customerSessionClientSecret: String = ""
)

internal class CustomFlowViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(
        value = CustomFlowViewState(cartState = CartState.defaultWithHardcodedPrices),
    )
    val state: StateFlow<CustomFlowViewState> = _state

    private val _addressState = MutableStateFlow(
        value = SelectedPartialAddress(
            city = "San Francisco",
            state = "CA",
            postalCode = "94107",
            country = "US"
        )
    )

    private var lineItems: List<FetchedItem> = emptyList()
    private var amountTotal: Int = 0

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

    fun handleShippingMethodUpdate(rate: SelectedShippingRate, handler: PaymentRequestShippingRateUpdateHandler) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val lineItems = fetchItems()
                val rates = fetchShippingRates(_addressState.value.state, _addressState.value.country)
                handler.onUpdate(
                    update = WalletConfiguration.PaymentRequestShippingRateUpdate.accepted(
                        lineItems = lineItems.items.map {
                            WalletConfiguration.LineItem(
                                name = it.name,
                                amount = it.amount
                            )
                        },
                        shippingRates = rates.map {
                            WalletConfiguration.ShippingRate(
                                id = it.id,
                                amount = it.amount,
                                displayName = it.displayName,
                                deliveryEstimate = WalletConfiguration.DeliveryEstimate.Text(it.deliveryEstimate)
                            )
                        }
                    )
                )
            } catch (e: Throwable) {
                handler.onUpdate(
                    update = WalletConfiguration.PaymentRequestShippingRateUpdate.rejected(
                        error = e.message ?: "Unknown error"
                    )
                )
            }
        }
    }

    fun handleShippingContactUpdate(address: SelectedPartialAddress, handler: PaymentRequestShippingContactUpdateHandler) {
        viewModelScope.launch(Dispatchers.IO) {
            _addressState.value = address
            val lineItems = fetchItems()
            val rates = fetchShippingRates(
                provinceCode = _addressState.value.state.takeIf { it.isNotBlank() } ?: "CA",
                countryCode = _addressState.value.country.takeIf { it.isNotBlank() } ?: "US"
            )
            handler.onUpdate(
                update = WalletConfiguration.PaymentRequestShippingContactUpdate(
                    lineItems = lineItems.items.map {
                        WalletConfiguration.LineItem(
                            name = it.name,
                            amount = it.amount
                        )
                    },
                    shippingRates = rates.map {
                        WalletConfiguration.ShippingRate(
                            id = it.id,
                            amount = it.amount,
                            displayName = it.displayName,
                            deliveryEstimate = WalletConfiguration.DeliveryEstimate.Text(it.deliveryEstimate)
                        )
                    }
                )
            )
        }
    }

    fun handlePaymentMethodInit(handler: PaymentMethodInitParamsHandler) {
        viewModelScope.launch(Dispatchers.IO) {
            val lineItems = fetchItems()
            val rates = fetchShippingRates(
                provinceCode = _addressState.value.state.takeIf { it.isNotBlank() } ?: "CA",
                countryCode = _addressState.value.country.takeIf { it.isNotBlank() } ?: "US"
            )

            handler(
                WalletConfiguration.PaymentMethodInitParams(
                    lineItems = lineItems.items.map {
                        WalletConfiguration.LineItem(
                            name = it.name,
                            amount = it.amount
                        )
                    },
                    shippingRates = rates.map {
                        WalletConfiguration.ShippingRate(
                            id = it.id,
                            amount = it.amount,
                            displayName = it.displayName,
                            deliveryEstimate = WalletConfiguration.DeliveryEstimate.Text(it.deliveryEstimate)
                        )
                    }
                )
            )
        }
    }

    private suspend fun fetchItems(): ItemsResponse {
        val url = "https://unexpected-dune-list.glitch.me/items"

        println("🛒 Fetching items from: $url")

        return try {
            val apiResult = Fuel.get(url)
                .suspendable()
                .awaitModel(ItemsResponse.serializer())

            when (apiResult) {
                is com.github.kittinunf.result.Result.Success -> {
                    val jsonString = apiResult.value

                    jsonString
                }
                is com.github.kittinunf.result.Result.Failure -> {
                    println("❌ Failed to fetch items: ${apiResult.error}")
                    useDefaultItems()
                }
            }
        } catch (error: Exception) {
            println("❌ Failed to fetch items: $error")
            useDefaultItems()
        }
    }

    private fun useDefaultItems(): ItemsResponse {
        // Use default values on error - matching the Swift code
        return ItemsResponse(
            items = listOf(
                FetchedItem(name = "Golden Potato", amount = 500, id = ""),
                FetchedItem(name = "Silver Potato", amount = 345, id = "")
            ),
            total = 1045
        )
    }

    private suspend fun fetchShippingRates(provinceCode: String, countryCode: String): List<ShippingRateResponseItem> {
        val url = "https://unexpected-dune-list.glitch.me/shipping?state=$provinceCode&country=$countryCode"

        println("📦 Fetching shipping rates from: $url")

        return try {
            val apiResult = Fuel.get(url)
                .suspendable()
                .awaitModel(ListSerializer(ShippingRateResponseItem.serializer()))

            when (apiResult) {
                is com.github.kittinunf.result.Result.Success -> {
                    val rates = apiResult.value
                    println("✅ Fetched ${rates.size} shipping rates")
                    rates
                }
                is com.github.kittinunf.result.Result.Failure -> {
                    println("❌ Failed to fetch shipping rates: ${apiResult.error}")
                    throw Exception("Failed to fetch shipping rates: ${apiResult.error}")
                }
            }
        } catch (error: Exception) {
            println("❌ Failed to fetch shipping rates: $error")
            throw error
        }
    }

    companion object {
        const val backendUrl = "https://stripe-mobile-payment-sheet.glitch.me"
    }
}
