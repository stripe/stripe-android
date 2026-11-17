@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.paymentsheet.example.playground.checkout

import android.app.Application
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stripe.android.PaymentConfiguration
import com.stripe.android.checkout.CheckoutElementsController
import com.stripe.android.elements.PaymentElement
import com.stripe.android.paymentsheet.example.Settings
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class CheckoutTwoStepExampleViewModel(
    private val application: Application,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val backend = PlaygroundBackend(
        baseUrl = Settings(application).playgroundBackendUrl,
        merchant = Merchant.US.value,
        customStripeApi = null,
        customSecretKey = null,
        customPublishableKey = null,
    )

    val controller = CheckoutElementsController.Builder(
        application = application,
        savedStateHandle = savedStateHandle,
    ).resultCallback(::onConfirmationResult).build()

    private val _status = MutableStateFlow(
        if (controller.session.value != null) Status.Configured else Status.Idle
    )
    val status: StateFlow<Status> = _status.asStateFlow()

    private val _confirmationMessage = MutableStateFlow<String?>(null)
    val confirmationMessage: StateFlow<String?> = _confirmationMessage.asStateFlow()

    private var checkoutJob: Job? = null

    fun startCheckout() {
        checkoutJob?.cancel()
        _confirmationMessage.value = null
        _status.value = Status.Loading
        checkoutJob = viewModelScope.launch {
            val clientSecret = runCatching {
                PaymentConfiguration.init(application, backend.fetchPublishableKey())
                backend.createCheckoutSession(CHECKOUT_SESSION_PARAMS)
            }.getOrElse { error ->
                Log.e(TAG, "Failed to create checkout session", error)
                _status.value = Status.Error(error.message ?: "Failed to create checkout session")
                return@launch
            }

            val result = controller.configure(
                clientSecret = clientSecret,
                configuration = CheckoutElementsController.Configuration()
                    .merchantDisplayName("Example, Inc.")
                    .paymentElement(PaymentElement.Configuration()),
            )
            _status.value = when (result) {
                is CheckoutElementsController.OperationResult.Succeeded -> Status.Configured
                is CheckoutElementsController.OperationResult.Failed -> {
                    Log.e(TAG, "Failed to configure", result.error)
                    Status.Error(result.displayMessage)
                }
            }
        }
    }

    fun cancelCheckout() {
        checkoutJob?.cancel()
        checkoutJob = null
        _confirmationMessage.value = null
        _status.value = Status.Idle
    }

    fun clearConfirmationMessage() {
        _confirmationMessage.value = null
    }

    private fun onConfirmationResult(result: CheckoutElementsController.Result) {
        Log.d(TAG, "Result: $result")
        when (result) {
            is CheckoutElementsController.Result.Completed -> _status.value = Status.Completed
            is CheckoutElementsController.Result.Canceled -> _confirmationMessage.value = "Confirmation canceled"
            is CheckoutElementsController.Result.Failed -> {
                _confirmationMessage.value = "Confirmation failed: ${result.displayMessage}"
            }
        }
    }

    override fun onCleared() {
        controller.destroy()
        super.onCleared()
    }

    sealed interface Status {
        data object Idle : Status
        data object Loading : Status
        data object Configured : Status
        data object Completed : Status
        data class Error(val message: String) : Status
    }

    companion object {
        private const val TAG = "CheckoutTwoStepExample"

        @Suppress("MagicNumber")
        val CART_ITEMS = listOf(
            CheckoutTwoStepCartItem(name = "Classic T-Shirt", unitAmount = 3500, quantity = 2),
            CheckoutTwoStepCartItem(name = "Zip-Up Hoodie", unitAmount = 5000, quantity = 1),
        )

        private val CHECKOUT_SESSION_PARAMS: JsonObject = buildJsonObject {
            put("ui_mode", "mobile_elements")
            put("currency", "usd")
            put("customer_email", "example@email.com")
            put(
                "items",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("type", "one_time_price")
                            put(
                                "one_time_price",
                                buildJsonObject {
                                    put(
                                        "items",
                                        buildJsonArray {
                                            CART_ITEMS.forEach { item -> add(item.toJson()) }
                                        },
                                    )
                                },
                            )
                        },
                    )
                },
            )
        }

        val factory = viewModelFactory {
            initializer {
                CheckoutTwoStepExampleViewModel(
                    application = this[APPLICATION_KEY] as Application,
                    savedStateHandle = createSavedStateHandle(),
                )
            }
        }
    }
}

internal data class CheckoutTwoStepCartItem(
    val name: String,
    val unitAmount: Int,
    val quantity: Int,
) {
    val displayPrice: String
        get() = "$%.2f".format(unitAmount * quantity / CENTS_PER_DOLLAR)

    fun toJson(): JsonObject = buildJsonObject {
        put(
            "price_data",
            buildJsonObject {
                put("currency", "usd")
                put("product_data", buildJsonObject { put("name", name) })
                put("unit_amount", unitAmount)
            },
        )
        put("quantity", quantity)
    }

    private companion object {
        const val CENTS_PER_DOLLAR = 100.0
    }
}
