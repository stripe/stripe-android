package com.stripe.android.paymentsheet.example.playground.customersheet

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.result.Result
import com.stripe.android.PaymentConfiguration
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.CustomerEphemeralKey
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetResult
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.paymentsheet.example.Settings
import com.stripe.android.paymentsheet.example.playground.customersheet.model.CreateSetupIntentRequest
import com.stripe.android.paymentsheet.example.playground.customersheet.model.CreateSetupIntentResponse
import com.stripe.android.paymentsheet.example.playground.customersheet.model.CustomerEphemeralKeyRequest
import com.stripe.android.paymentsheet.example.playground.customersheet.model.CustomerEphemeralKeyResponse
import com.stripe.android.paymentsheet.example.playground.customersheet.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.customersheet.settings.CustomerSheetPlaygroundSettings
import com.stripe.android.paymentsheet.example.playground.customersheet.settings.PaymentMethodMode
import com.stripe.android.paymentsheet.example.playground.customersheet.settings.PaymentMethodModeDefinition
import com.stripe.android.paymentsheet.example.samples.networking.awaitModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

internal class CustomerSheetPlaygroundViewModel(
    application: Application,
    launchUri: Uri?,
) : AndroidViewModel(application) {
    private val settings by lazy {
        Settings(application)
    }

    val playgroundSettingsFlow = MutableStateFlow<CustomerSheetPlaygroundSettings?>(null)
    val playgroundState = MutableStateFlow<CustomerSheetPlaygroundState?>(null)

    val status = MutableSharedFlow<StatusMessage>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            playgroundSettingsFlow.value =
                CustomerSheetPlaygroundUrlHelper.settingsFromUri(launchUri)
                    ?: CustomerSheetPlaygroundSettings.createFromSharedPreferences(application)
        }
    }

    @OptIn(ExperimentalCustomerSheetApi::class)
    fun reset() {
        val playgroundSettings = playgroundSettingsFlow.value ?: return

        val setupIntentClientSecretProvider = if (
            playgroundSettings[PaymentMethodModeDefinition].value == PaymentMethodMode.SetupIntent
        ) {
            ::createSetupIntentClientSecret
        } else {
            null
        }

        playgroundState.value = CustomerSheetPlaygroundState(
            snapshot = playgroundSettings.snapshot(),
            adapter = CustomerAdapter.create(
                context = getApplication(),
                customerEphemeralKeyProvider = ::fetchEphemeralKey,
                setupIntentClientSecretProvider = setupIntentClientSecretProvider

            ),
            optionState = CustomerSheetPlaygroundState.PaymentOptionState.Unloaded
        )
    }

    @OptIn(ExperimentalCustomerSheetApi::class)
    fun fetchOption(customerSheet: CustomerSheet) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = customerSheet.retrievePaymentOptionSelection()) {
                is CustomerSheetResult.Selected -> {
                    playgroundState.update { existingState ->
                        existingState?.let { state ->
                            return@update state.copy(
                                optionState = CustomerSheetPlaygroundState.PaymentOptionState.Loaded(
                                    paymentOption = result.selection?.paymentOption
                                )
                            )
                        }
                    }
                }
                is CustomerSheetResult.Failed -> {
                    status.emit(
                        StatusMessage(
                            message = "Failed to retrieve payment options:\n${result.exception.message}"
                        )
                    )
                }
                is CustomerSheetResult.Canceled -> Unit
            }
        }
    }

    @OptIn(ExperimentalCustomerSheetApi::class)
    fun onCustomerSheetCallback(result: CustomerSheetResult) {
        val statusMessage = when (result) {
            is CustomerSheetResult.Selected -> {
                playgroundState.update { existingState ->
                    existingState?.let { state ->
                        if (state.optionState is CustomerSheetPlaygroundState.PaymentOptionState.Loaded) {
                            return@update state.copy(
                                optionState = CustomerSheetPlaygroundState.PaymentOptionState.Loaded(
                                    paymentOption = result.selection?.paymentOption
                                )
                            )
                        }
                    }

                    existingState
                }

                null
            }
            is CustomerSheetResult.Failed -> "An error occurred: ${result.exception.message}"
            is CustomerSheetResult.Canceled -> "Canceled"
        }

        statusMessage?.let { message ->
            viewModelScope.launch {
                status.emit(StatusMessage(message))
            }
        }
    }

    @OptIn(ExperimentalCustomerSheetApi::class)
    private suspend fun fetchEphemeralKey(): CustomerAdapter.Result<CustomerEphemeralKey> {
        val playgroundSettings = playgroundSettingsFlow.value
            ?: return CustomerAdapter.Result.failure(
                cause = Exception("Unexpected error: Playground settings were not found!"),
                displayMessage = null,
            )

        // Snapshot before making the network request to not rely on UI staying in sync.
        val playgroundSettingsSnapshot = playgroundSettings.snapshot()

        playgroundSettingsSnapshot.saveToSharedPreferences(getApplication())

        val requestBody = playgroundSettingsSnapshot.customerEphemeralKeyRequest()

        val apiResponse = Fuel.post(settings.playgroundBackendUrl + "customer_ephemeral_key")
            .jsonBody(Json.encodeToString(CustomerEphemeralKeyRequest.serializer(), requestBody))
            .suspendable()
            .awaitModel(CustomerEphemeralKeyResponse.serializer())

        return when (apiResponse) {
            is Result.Failure -> {
                val exception = apiResponse.getException()

                CustomerAdapter.Result.failure(
                    cause = exception,
                    displayMessage = "Failed to fetch ephemeral key:\n${exception.message}"
                )
            }
            is Result.Success -> {
                val response = apiResponse.value

                // Init PaymentConfiguration with the publishable key returned from the backend,
                // which will be used on all Stripe API calls
                PaymentConfiguration.init(
                    getApplication(),
                    response.publishableKey
                )

                try {
                    CustomerAdapter.Result.success(
                        CustomerEphemeralKey.create(
                            customerId = response.customerId,
                            ephemeralKey = response.customerEphemeralKeySecret
                                ?: throw IllegalStateException(
                                    "No 'customerEphemeralKeySecret' was found in backend response!"
                                )
                        )
                    )
                } catch (exception: IllegalStateException) {
                    CustomerAdapter.Result.failure(
                        cause = exception,
                        displayMessage = "Failed to fetch ephemeral key:\n${exception.message}",
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalCustomerSheetApi::class)
    private suspend fun createSetupIntentClientSecret(customerId: String): CustomerAdapter.Result<String> {
        val playgroundSettings = playgroundSettingsFlow.value
            ?: return CustomerAdapter.Result.failure(
                cause = Exception("Unexpected error: Playground settings were not found!"),
                displayMessage = null,
            )

        val request = CreateSetupIntentRequest(
            customerId = customerId,
            merchantCountryCode = playgroundSettings[CountrySettingsDefinition].value.value,
        )

        val apiResponse = Fuel.post(settings.playgroundBackendUrl + "create_setup_intent")
            .jsonBody(Json.encodeToString(CreateSetupIntentRequest.serializer(), request))
            .suspendable()
            .awaitModel(CreateSetupIntentResponse.serializer())

        return when (apiResponse) {
            is Result.Failure -> {
                val exception = apiResponse.getException()

                CustomerAdapter.Result.failure(
                    cause = exception,
                    displayMessage = "Failed to fetch setup intent secret:\n${exception.message}"
                )
            }
            is Result.Success -> CustomerAdapter.Result.success(apiResponse.value.clientSecret)
        }
    }

    internal class Factory(
        private val applicationSupplier: () -> Application,
        private val uriSupplier: () -> Uri?,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return CustomerSheetPlaygroundViewModel(applicationSupplier(), uriSupplier()) as T
        }
    }
}

data class StatusMessage(
    val message: String,
)
