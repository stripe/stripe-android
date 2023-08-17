package com.stripe.android.paymentsheet.example.samples.ui.customersheet.playground

import android.app.Application
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
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

    private val _configurationState = MutableStateFlow(CustomerSheetPlaygroundConfigurationState())
    val configurationState: StateFlow<CustomerSheetPlaygroundConfigurationState> = _configurationState

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
        .googlePayEnabled(configurationState.value.isGooglePayEnabled)
        .build()
    val configuration: StateFlow<CustomerSheet.Configuration> = configurationState.map {
        initialConfiguration
            .newBuilder()
            .defaultBillingDetails(
                if (it.useDefaultBillingAddress) {
                    PaymentSheet.BillingDetails(
                        address = PaymentSheet.Address(
                            city = "Seattle",
                            country = "US",
                            line1 = "123 Main St.",
                            postalCode = "99999",
                        )
                    )
                } else {
                    PaymentSheet.BillingDetails()
                }
            )
            .billingDetailsCollectionConfiguration(
                configuration = PaymentSheet.BillingDetailsCollectionConfiguration(
                    name = it.billingCollectionConfiguration.name,
                    phone = it.billingCollectionConfiguration.phone,
                    email = it.billingCollectionConfiguration.email,
                    address = it.billingCollectionConfiguration.address,
                    attachDefaultsToPaymentMethod = it.attachDefaultBillingAddress,
                )
            )
            .googlePayEnabled(it.isGooglePayEnabled)
            .build()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), initialConfiguration)

    internal val customerAdapter: StateFlow<CustomerSheetPlaygroundAdapter> = configurationState.map {
        CustomerSheetPlaygroundAdapter(
            overrideCanCreateSetupIntents = it.isSetupIntentEnabled,
            adapter = CustomerAdapter.create(
                context = getApplication(),
                customerEphemeralKeyProvider = {
                    fetchCustomerEphemeralKey(viewState.value.currentCustomerId).fold(
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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = CustomerSheetPlaygroundAdapter(
            adapter = CustomerAdapter.create(
                context = getApplication(),
                customerEphemeralKeyProvider = {
                    CustomerAdapter.Result.failure(
                        cause = NotImplementedError(),
                        displayMessage = null
                    )
                },
                setupIntentClientSecretProvider = null
            )
        )
    )

    init {
        viewModelScope.launch {
            fetchCustomerEphemeralKey()
        }
    }

    private suspend fun fetchCustomerEphemeralKey(
        customerId: String = "returning"
    ): Result<ExampleCustomerSheetResponse, FuelError> {
        return withContext(Dispatchers.IO) {
            val request = ExampleCustomerSheetRequest(
                customerType = customerId
            )
            val requestBody = Json.encodeToString(
                ExampleCustomerSheetRequest.serializer(),
                request
            )

            val result = Fuel
                .post("$backendUrl/customer_ephemeral_key")
                .jsonBody(requestBody)
                .suspendable()
                .awaitModel(ExampleCustomerSheetResponse.serializer())

            when (result) {
                is Result.Success -> {
                    PaymentConfiguration.init(
                        context = getApplication(),
                        publishableKey = result.value.publishableKey,
                    )
                    _viewState.update {
                        CustomerSheetPlaygroundViewState.Data(
                            currentCustomer = result.value.customerId,
                        )
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

            result
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
            is CustomerSheetPlaygroundViewAction.ToggleGooglePayEnabled ->
                toggleGooglePayEnabled()
            is CustomerSheetPlaygroundViewAction.ToggleSetupIntentEnabled ->
                toggleSetupIntentEnabled()
            is CustomerSheetPlaygroundViewAction.ToggleExistingCustomer ->
                toggleExistingCustomer()
            is CustomerSheetPlaygroundViewAction.ToggleUseDefaultBillingAddress ->
                toggleUseDefaultBillingAddress()
            is CustomerSheetPlaygroundViewAction.ToggleAttachDefaultBillingAddress ->
                toggleAttachDefaultBillingAddress()
            is CustomerSheetPlaygroundViewAction.UpdateBillingAddressCollection ->
                updateBillingAddressCollection(viewAction.value)
            is CustomerSheetPlaygroundViewAction.UpdateBillingEmailCollection ->
                updateBillingEmailCollection(viewAction.value)
            is CustomerSheetPlaygroundViewAction.UpdateBillingNameCollection ->
                updateBillingNameCollection(viewAction.value)
            is CustomerSheetPlaygroundViewAction.UpdateBillingPhoneCollection ->
                updateBillingPhoneCollection(viewAction.value)
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
        updateConfiguration {
            it.copy(
                isSetupIntentEnabled = !it.isSetupIntentEnabled
            )
        }
    }

    private fun toggleGooglePayEnabled() {
        updateConfiguration {
            it.copy(
                isGooglePayEnabled = !it.isGooglePayEnabled
            )
        }
    }

    private fun toggleExistingCustomer() {
        updateConfiguration {
            it.copy(
                isExistingCustomer = !it.isExistingCustomer,
            )
        }

        viewModelScope.launch {
            fetchCustomerEphemeralKey(
                if (configurationState.value.isExistingCustomer) {
                    "returning"
                } else {
                    "new"
                }
            )
        }
    }

    private fun toggleUseDefaultBillingAddress() {
        updateConfiguration {
            it.copy(
                useDefaultBillingAddress = !it.useDefaultBillingAddress
            )
        }
    }

    private fun toggleAttachDefaultBillingAddress() {
        updateConfiguration {
            it.copy(
                attachDefaultBillingAddress = !it.attachDefaultBillingAddress
            )
        }
    }

    private fun updateBillingAddressCollection(value: String) {
        updateConfiguration {
            it.copy(
                billingCollectionConfiguration = it.billingCollectionConfiguration.copy(
                    address =
                    PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.valueOf(
                        value.capitalize(Locale.current)
                    )
                )
            )
        }
    }

    private fun updateBillingEmailCollection(value: String) {
        updateConfiguration {
            it.copy(
                billingCollectionConfiguration = it.billingCollectionConfiguration.copy(
                    email =
                    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.valueOf(
                        value.capitalize(Locale.current)
                    )
                )
            )
        }
    }

    private fun updateBillingNameCollection(value: String) {
        updateConfiguration {
            it.copy(
                billingCollectionConfiguration = it.billingCollectionConfiguration.copy(
                    name =
                    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.valueOf(
                        value.capitalize(Locale.current)
                    )
                )
            )
        }
    }

    private fun updateBillingPhoneCollection(value: String) {
        updateConfiguration {
            it.copy(
                billingCollectionConfiguration = it.billingCollectionConfiguration.copy(
                    phone =
                    PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.valueOf(
                        value.capitalize(Locale.current)
                    )
                )
            )
        }
    }

    private inline fun <reified T : CustomerSheetPlaygroundViewState> updateViewState(transform: (T) -> T) {
        (_viewState.value as? T)?.let {
            _viewState.update {
                transform(it as T)
            }
        }
    }

    private inline fun updateConfiguration(
        transform: (CustomerSheetPlaygroundConfigurationState) -> CustomerSheetPlaygroundConfigurationState
    ) {
        _configurationState.update {
            transform(it)
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
        const val backendUrl = "https://stp-mobile-playground-backend-v7.stripedemos.com"
    }
}
