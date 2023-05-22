package com.stripe.android.paymentsheet.example.samples.ui.customer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.stripe.android.ExperimentalCustomerSheetApi
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCreateSetupIntentRequest
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCreateSetupIntentResponse
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCustomerRequest
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCustomerResponse
import com.stripe.android.paymentsheet.example.samples.networking.awaitModel
import com.stripe.android.paymentsheet.repositories.CustomerAdapter
import com.stripe.android.paymentsheet.repositories.CustomerEphemeralKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import com.github.kittinunf.result.Result as FuelResult

@OptIn(ExperimentalCustomerSheetApi::class)
class CustomerViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _state = MutableStateFlow<CustomerViewState>(
        value = CustomerViewState.Loading
    )
    val state: StateFlow<CustomerViewState> = _state

    internal val customerAdapter: CustomerAdapter = CustomerAdapter.create(
        context = getApplication(),
        customerEphemeralKeyProvider = {
            fetchCustomerEphemeralKey().fold(
                success = {
                    Result.success(
                        CustomerEphemeralKey.create(
                            customerId = it.customerId,
                            ephemeralKey = it.customerEphemeralKeySecret
                        )
                    )
                },
                failure = {
                    Result.failure(it.exception)
                }
            )
        },
        setupIntentClientSecretProvider = { customerId ->
            createSetupIntent(customerId).fold(
                success = {
                    Result.success(
                        it.clientSecret
                    )
                },
                failure = {
                    Result.failure(it.exception)
                }
            )
        },
    )

    init {
        viewModelScope.launch {
            initialize()
        }
    }

    private suspend fun initialize() {
        when (val result = fetchCustomerEphemeralKey()) {
            is FuelResult.Success -> {
                PaymentConfiguration.init(
                    context = getApplication(),
                    publishableKey = result.value.publishableKey,
                )
                _state.update {
                    CustomerViewState.Data(
                        customerEphemeralKey = CustomerEphemeralKey.create(
                            customerId = result.value.customerId,
                            ephemeralKey = result.value.customerEphemeralKeySecret,
                        )
                    )
                }
            }
            is FuelResult.Failure -> {
                _state.update {
                    CustomerViewState.FailedToLoad(
                        message = result.error.message.toString()
                    )
                }
            }
        }
    }

    private suspend fun fetchCustomerEphemeralKey():
        FuelResult<ExampleCustomerResponse, FuelError> {
        val request = ExampleCustomerRequest(
            customerType = "returning"
        )
        val requestBody = Json.encodeToString(
            ExampleCustomerRequest.serializer(),
            request
        )

        return Fuel
            .post("$backendUrl/customer_ephemeral_key")
            .jsonBody(requestBody)
            .suspendable()
            .awaitModel(ExampleCustomerResponse.serializer())
    }

    private suspend fun createSetupIntent(customerId: String):
        FuelResult<ExampleCreateSetupIntentResponse, FuelError> {
        val request = ExampleCreateSetupIntentRequest(
            customerId = customerId
        )
        val requestBody = Json.encodeToString(
            ExampleCreateSetupIntentRequest.serializer(),
            request
        )

        return Fuel
            .post("$backendUrl/create_setup_intent")
            .jsonBody(requestBody)
            .suspendable()
            .awaitModel(ExampleCreateSetupIntentResponse.serializer())
    }

    private companion object {
        const val backendUrl = "https://glistening-heavenly-radon.glitch.me"
    }
}
