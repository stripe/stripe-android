package com.stripe.android.paymentsheet.example.samples.ui.customersheet.playground

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.result.Result
import com.stripe.android.PaymentConfiguration
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.CustomerEphemeralKey
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetResult
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCreateSetupIntentRequest
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCreateSetupIntentResponse
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCustomerSheetRequest
import com.stripe.android.paymentsheet.example.samples.networking.ExampleCustomerSheetResponse
import com.stripe.android.paymentsheet.example.samples.networking.awaitModel
import com.stripe.android.utils.requireApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@OptIn(ExperimentalCustomerSheetApi::class)
class CustomerSheetPlaygroundViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val _viewState =
        MutableStateFlow<CustomerSheetPlaygroundViewState>(CustomerSheetPlaygroundViewState.Loading)
    val viewState: StateFlow<CustomerSheetPlaygroundViewState> = _viewState

    private val initialConfiguration = CustomerSheet.Configuration.Builder()
        .defaultBillingDetails(
            PaymentSheet.BillingDetails(
                name = "CustomerSheet Testing"
            )
        ).billingDetailsCollectionConfiguration(
            PaymentSheet.BillingDetailsCollectionConfiguration(
                name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
            )
        )
        .googlePayEnabled(viewState.value.isGooglePayEnabled)
        .build()
    val configuration: StateFlow<CustomerSheet.Configuration> = viewState.map {
        CustomerSheet.Configuration.Builder()
            .defaultBillingDetails(
                PaymentSheet.BillingDetails(
                    name = "CustomerSheet Testing"
                )
            ).billingDetailsCollectionConfiguration(
                PaymentSheet.BillingDetailsCollectionConfiguration(
                    name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always
                )
            )
            .googlePayEnabled(it.isGooglePayEnabled)
            .build()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), initialConfiguration)

    private val initialCustomerAdapter = CustomerSheetPlaygroundAdapter(
        overrideCanCreateSetupIntents = viewState.value.isSetupIntentEnabled,
        adapter = CustomerAdapter.create(
            context = getApplication(),
            customerEphemeralKeyProvider = {
                val isExistingCustomer = viewState.value.isExistingCustomer
                fetchCustomerEphemeralKey(isExistingCustomer).fold(
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
                            "We couldn't retrieve your information, please try again."
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
                            "We couldn't retrieve your information, please try again."
                        )
                    }
                )
            },
        )
    )

    internal val customerAdapter: StateFlow<CustomerSheetPlaygroundAdapter> = viewState.map {
        CustomerSheetPlaygroundAdapter(
            overrideCanCreateSetupIntents = it.isSetupIntentEnabled,
            adapter = CustomerAdapter.create(
                context = getApplication(),
                customerEphemeralKeyProvider = {
                    fetchCustomerEphemeralKey(it.isExistingCustomer).fold(
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
                                "We couldn't retrieve your information, please try again."
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
                                "We couldn't retrieve your information, please try again."
                            )
                        }
                    )
                },
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), initialCustomerAdapter)

    init {
        viewModelScope.launch {
            initialize()
        }
    }

    private suspend fun initialize() {
        when (val result = fetchCustomerEphemeralKey(viewState.value.isExistingCustomer)) {
            is Result.Success -> {
                PaymentConfiguration.init(
                    context = getApplication(),
                    publishableKey = result.value.publishableKey,
                )
                _viewState.update {
                    CustomerSheetPlaygroundViewState.Data()
                }
            }

            is Result.Failure -> {
                _viewState.update {
                    CustomerSheetPlaygroundViewState.FailedToLoad(
                        message = result.error.message.toString()
                    )
                }
            }
        }
    }

    private suspend fun fetchCustomerEphemeralKey(
        isExistingCustomer: Boolean
    ): Result<ExampleCustomerSheetResponse, FuelError> {
        return withContext(Dispatchers.IO) {
            val request = ExampleCustomerSheetRequest(
                customerType = if (isExistingCustomer) {
                    "returning"
                } else {
                    "new"
                }
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
        Result<ExampleCreateSetupIntentResponse, FuelError> {
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

    fun handleViewAction(viewAction: CustomerSheetPlaygroundViewAction) {
        when (viewAction) {
            CustomerSheetPlaygroundViewAction.ToggleGooglePayEnabled -> toggleGooglePayEnabled()
            CustomerSheetPlaygroundViewAction.ToggleSetupIntentEnabled -> toggleSetupIntentEnabled()
            CustomerSheetPlaygroundViewAction.ToggleExistingCustomer -> toggleExistingCustomer()
        }
    }

    fun onCustomerSheetResult(result: CustomerSheetResult) {
        when (result) {
            is CustomerSheetResult.Canceled -> {
                updateViewState<CustomerSheetPlaygroundViewState.Data> {
                    it.copy(
                        selection = result.selection,
                        errorMessage = null,
                    )
                }
            }
            is CustomerSheetResult.Selected -> {
                updateViewState<CustomerSheetPlaygroundViewState.Data> {
                    it.copy(
                        selection = result.selection,
                        errorMessage = null,
                    )
                }
            }
            is CustomerSheetResult.Error -> {
                updateViewState<CustomerSheetPlaygroundViewState.Data> {
                    it.copy(
                        selection = null,
                        errorMessage = result.exception.message,
                    )
                }
            }
        }
    }

    private fun toggleSetupIntentEnabled() {
        updateViewState<CustomerSheetPlaygroundViewState.Data> {
            it.copy(
                isSetupIntentEnabled = !it.isSetupIntentEnabled
            )
        }
    }

    private fun toggleGooglePayEnabled() {
        updateViewState<CustomerSheetPlaygroundViewState.Data> {
            it.copy(
                isGooglePayEnabled = !it.isGooglePayEnabled
            )
        }
    }

    private fun toggleExistingCustomer() {
        updateViewState<CustomerSheetPlaygroundViewState.Data> {
            it.copy(
                isExistingCustomer = !it.isExistingCustomer,
            )
        }

        viewModelScope.launch {
            when (val result = fetchCustomerEphemeralKey(viewState.value.isExistingCustomer)) {
                is Result.Success -> {
                    PaymentConfiguration.init(
                        context = getApplication(),
                        publishableKey = result.value.publishableKey,
                    )
                }
                is Result.Failure -> {
                    _viewState.update {
                        CustomerSheetPlaygroundViewState.FailedToLoad(
                            message = result.error.message.toString()
                        )
                    }
                }
            }
        }
    }

    private inline fun <reified T : CustomerSheetPlaygroundViewState> updateViewState(block: (T) -> T) {
        (_viewState.value as? T)?.let {
            _viewState.update {
                block(it as T)
            }
        }
    }

    object Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return CustomerSheetPlaygroundViewModel(
                application = extras.requireApplication(),
            ) as T
        }
    }

    private companion object {
        const val backendUrl = "https://glistening-heavenly-radon.glitch.me"
    }
}
