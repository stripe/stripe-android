package com.stripe.android.paymentsheet.example.samples.ui.saved_pms

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.result.Result
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.example.samples.networking.ExampleSavedPaymentMethodRequest
import com.stripe.android.paymentsheet.example.samples.networking.ExampleSavedPaymentMethodResponse
import com.stripe.android.paymentsheet.example.samples.networking.awaitModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class SavedPaymentMethodsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(
        value = SavedPaymentMethodsViewState()
    )
    val state: StateFlow<SavedPaymentMethodsViewState> = _state

    fun updateCustomerType(type: SavedPaymentMethodsViewState.CustomerType) {
        _state.update {
            it.copy(customerType = type)
        }
        fetchSavedPaymentMethods()
    }

    suspend fun fetchEphemeralKey(): String {
        val currentState = _state.updateAndGet {
            it.copy(isProcessing = true)
        }
        val request = ExampleSavedPaymentMethodRequest(
            customerType = currentState.customerType.value
        )
        val requestBody = Json.encodeToString(
            ExampleSavedPaymentMethodRequest.serializer(),
            request
        )
        val apiResult = Fuel
            .post("${backendUrl}/customer_ephemeral_key")
            .jsonBody(requestBody)
            .suspendable()
            .awaitModel(ExampleSavedPaymentMethodResponse.serializer())

        return when (apiResult) {
            is Result.Success -> {
                apiResult.value.customerEphemeralKeySecret
            }
            is Result.Failure -> {
                ""
            }
        }
    }

    private fun fetchSavedPaymentMethods() {
        viewModelScope.launch(Dispatchers.IO) {
            println("JAMES: fetching customer")
            val currentState = _state.updateAndGet {
                it.copy(isProcessing = true)
            }
            val request = ExampleSavedPaymentMethodRequest(
                customerType = currentState.customerType.value
            )
            val requestBody = Json.encodeToString(
                ExampleSavedPaymentMethodRequest.serializer(),
                request
            )
            val apiResult = Fuel
                .post("${backendUrl}/saved_payment_method")
                .jsonBody(requestBody)
                .suspendable()
                .awaitModel(ExampleSavedPaymentMethodResponse.serializer())

            when (apiResult) {
                is Result.Success -> {
                    PaymentConfiguration.init(
                        context = getApplication(),
                        publishableKey = apiResult.value.publishableKey,
                    )
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            customerState = SavedPaymentMethodsViewState.CustomerState(
                                customerId = apiResult.value.customerId,
                            )
                        )
                    }
                }
                is Result.Failure -> {
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            customerState = SavedPaymentMethodsViewState.CustomerState.empty()
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val backendUrl = "https://glistening-heavenly-radon.glitch.me"
    }
}
