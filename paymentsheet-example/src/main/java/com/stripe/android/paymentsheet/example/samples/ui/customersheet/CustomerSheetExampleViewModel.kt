package com.stripe.android.paymentsheet.example.samples.ui.customersheet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.stripe.android.PaymentConfiguration
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.CustomerEphemeralKey
import com.stripe.android.customersheet.CustomerSheetResult
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCreateSetupIntentRequest
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCreateSetupIntentResponse
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCustomerSheetRequest
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCustomerSheetResponse
import com.stripe.android.paymentsheet.example.samples.networking.awaitModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import com.github.kittinunf.result.Result as FuelResult

@OptIn(ExperimentalCustomerSheetApi::class)
class CustomerSheetExampleViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _state = MutableStateFlow<CustomerSheetExampleViewState>(
        value = CustomerSheetExampleViewState.Loading
    )
    val state: StateFlow<CustomerSheetExampleViewState> = _state

    private val _isSetupIntentEnabled = MutableStateFlow(true)
    val isSetupIntentEnabled: StateFlow<Boolean> = _isSetupIntentEnabled

    private val _isDeveloperModeEnabled = MutableStateFlow(false)
    val isDeveloperModeEnabled: StateFlow<Boolean> = _isDeveloperModeEnabled

    private val _customerAdapter: CustomerAdapter = CustomerAdapter.create(
        context = getApplication(),
        customerEphemeralKeyProvider = {
            fetchCustomerEphemeralKey().fold(
                success = {
                    CustomerAdapter.Result.success(
                        CustomerEphemeralKey.create(
                            customerId = it.customerId,
                            ephemeralKey = it.customerEphemeralKeySecret
                        )
                    )
                },
                failure = {
                    CustomerAdapter.Result.failure(
                        it.exception,
                        "We could\'nt retrieve your information, please try again."
                    )
                }
            )
        },
        setupIntentClientSecretProvider = { customerId ->
            createSetupIntent(customerId).fold(
                success = {
                    CustomerAdapter.Result.success(
                        it.clientSecret
                    )
                },
                failure = {
                    CustomerAdapter.Result.failure(
                        it.exception,
                        "We could\'nt retrieve your information, please try again."
                    )
                }
            )
        },
    )

    internal val customerAdapter = CustomerSheetExampleAdapter(
        _customerAdapter
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
                    CustomerSheetExampleViewState.Data(
                        customerEphemeralKey = CustomerEphemeralKey.create(
                            customerId = result.value.customerId,
                            ephemeralKey = result.value.customerEphemeralKeySecret,
                        )
                    )
                }
            }
            is FuelResult.Failure -> {
                _state.update {
                    CustomerSheetExampleViewState.FailedToLoad(
                        message = result.error.message.toString()
                    )
                }
            }
        }
    }

    private suspend fun fetchCustomerEphemeralKey():
        FuelResult<ExampleCustomerSheetResponse, FuelError> {
        return withContext(Dispatchers.IO) {
            val request = ExampleCustomerSheetRequest(
                customerType = "returning"
            )
            val requestBody = Json.encodeToString(
                ExampleCustomerSheetRequest.serializer(),
                request
            )

            Fuel
                .post("$backendUrl/customer_ephemeral_key")
                .jsonBody(requestBody)
                .suspendable()
                .awaitModel(ExampleCustomerSheetResponse.serializer())
        }
    }

    private suspend fun createSetupIntent(customerId: String):
        FuelResult<ExampleCreateSetupIntentResponse, FuelError> {
        return withContext(Dispatchers.IO) {
            val request = ExampleCreateSetupIntentRequest(
                customerId = customerId
            )
            val requestBody = Json.encodeToString(
                ExampleCreateSetupIntentRequest.serializer(),
                request
            )

            Fuel
                .post("$backendUrl/create_setup_intent")
                .jsonBody(requestBody)
                .suspendable()
                .awaitModel(ExampleCreateSetupIntentResponse.serializer())
        }
    }

    fun onCustomerSheetResult(result: CustomerSheetResult) {
        when (result) {
            is CustomerSheetResult.Canceled -> {
                updateDataViewState {
                    it.copy(
                        selection = result.selection,
                        errorMessage = null,
                    )
                }
            }
            is CustomerSheetResult.Selected -> {
                updateDataViewState {
                    it.copy(
                        selection = result.selection,
                        errorMessage = null,
                    )
                }
            }
            is CustomerSheetResult.Error -> {
                updateDataViewState {
                    it.copy(
                        selection = null,
                        errorMessage = result.exception.message,
                    )
                }
            }
        }
    }

    fun toggleSetupIntentEnabled(isSetupIntentEnabled: Boolean) {
        _isSetupIntentEnabled.update { isSetupIntentEnabled }
        customerAdapter.overrideCanCreateSetupIntents = isSetupIntentEnabled
    }

    fun toggleDeveloperMode() {
        _isDeveloperModeEnabled.update { !it }
    }

    private fun updateDataViewState(
        transform: (CustomerSheetExampleViewState.Data) -> CustomerSheetExampleViewState.Data,
    ) {
        _state.update {
            if (it is CustomerSheetExampleViewState.Data) {
                transform(it)
            } else {
                it
            }
        }
    }

    private companion object {
        const val backendUrl = "https://glistening-heavenly-radon.glitch.me"
    }
}
