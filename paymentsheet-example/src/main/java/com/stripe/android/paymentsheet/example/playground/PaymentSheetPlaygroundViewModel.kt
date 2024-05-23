package com.stripe.android.paymentsheet.example.playground

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
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.DelicatePaymentSheetApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.addresselement.AddressLauncherResult
import com.stripe.android.paymentsheet.example.Settings
import com.stripe.android.paymentsheet.example.playground.PlaygroundState.Companion.asPlaygroundState
import com.stripe.android.paymentsheet.example.playground.model.CheckoutRequest
import com.stripe.android.paymentsheet.example.playground.model.CheckoutResponse
import com.stripe.android.paymentsheet.example.playground.model.ConfirmIntentRequest
import com.stripe.android.paymentsheet.example.playground.model.ConfirmIntentResponse
import com.stripe.android.paymentsheet.example.playground.model.CreateSetupIntentRequest
import com.stripe.android.paymentsheet.example.playground.model.CreateSetupIntentResponse
import com.stripe.android.paymentsheet.example.playground.model.CustomerEphemeralKeyRequest
import com.stripe.android.paymentsheet.example.playground.model.CustomerEphemeralKeyResponse
import com.stripe.android.paymentsheet.example.playground.settings.CountrySettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSheetPaymentMethodModeDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.InitializationType
import com.stripe.android.paymentsheet.example.playground.settings.PaymentMethodMode
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundSettings
import com.stripe.android.paymentsheet.example.playground.settings.ShippingAddressSettingsDefinition
import com.stripe.android.paymentsheet.example.samples.networking.awaitModel
import com.stripe.android.paymentsheet.model.PaymentOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.IOException

internal class PaymentSheetPlaygroundViewModel(
    application: Application,
    launchUri: Uri?,
) : AndroidViewModel(application) {
    private val settings by lazy {
        Settings(application)
    }

    val playgroundSettingsFlow = MutableStateFlow<PlaygroundSettings?>(null)
    val status = MutableStateFlow<StatusMessage?>(null)
    val state = MutableStateFlow<PlaygroundState?>(null)
    val flowControllerState = MutableStateFlow<FlowControllerState?>(null)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            playgroundSettingsFlow.value =
                PaymentSheetPlaygroundUrlHelper.settingsFromUri(launchUri)
                    ?: PlaygroundSettings.createFromSharedPreferences(application)
        }
    }

    fun prepare(
        playgroundSettings: PlaygroundSettings,
    ) {
        state.value = null
        flowControllerState.value = null

        if (playgroundSettings.configurationData.value.integrationType.isPaymentFlow()) {
            prepareCheckout(playgroundSettings)
        } else {
            prepareCustomer(playgroundSettings)
        }
    }

    /**
     * Calls the backend to prepare for checkout. The server creates a new Payment or Setup Intent
     * that will be confirmed on the client using Payment Sheet.
     */
    private fun prepareCheckout(
        playgroundSettings: PlaygroundSettings,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // Snapshot before making the network request to not rely on UI staying in sync.
            val playgroundSettingsSnapshot = playgroundSettings.snapshot()

            playgroundSettingsSnapshot.saveToSharedPreferences(getApplication())

            val requestBody = playgroundSettingsSnapshot.checkoutRequest()

            val apiResponse = Fuel.post(settings.playgroundBackendUrl + "checkout")
                .jsonBody(Json.encodeToString(CheckoutRequest.serializer(), requestBody))
                .suspendable()
                .awaitModel(CheckoutResponse.serializer())
            when (apiResponse) {
                is Result.Failure -> {
                    status.value = StatusMessage(
                        "Preparing checkout failed:\n${apiResponse.getException().message}"
                    )
                }

                is Result.Success -> {
                    val checkoutResponse = apiResponse.value
                    println("StripeIntent ${checkoutResponse.intentClientSecret.substringBefore("_secret_")}")

                    // Init PaymentConfiguration with the publishable key returned from the backend,
                    // which will be used on all Stripe API calls
                    PaymentConfiguration.init(
                        getApplication(),
                        checkoutResponse.publishableKey
                    )

                    val customerId = checkoutResponse.customerId
                    val updatedSettings = playgroundSettingsSnapshot.playgroundSettings()
                    if (
                        playgroundSettingsSnapshot[CustomerSettingsDefinition] == CustomerType.NEW &&
                        customerId != null
                    ) {
                        println("Customer $customerId")
                        updatedSettings[CustomerSettingsDefinition] = CustomerType.Existing(customerId)
                    }
                    playgroundSettingsFlow.value = updatedSettings
                    val updatedState = checkoutResponse.asPlaygroundState(
                        snapshot = updatedSettings.snapshot(),
                    )
                    state.value = updatedState
                }
            }
        }
    }

    @OptIn(ExperimentalCustomerSheetApi::class)
    private fun prepareCustomer(
        playgroundSettings: PlaygroundSettings
    ) {
        viewModelScope.launch {
            val snapshot = playgroundSettings.snapshot()

            snapshot.saveToSharedPreferences(getApplication())

            state.value = PlaygroundState.Customer(
                snapshot = snapshot,
                adapter = CustomerAdapter.create(
                    context = getApplication(),
                    customerEphemeralKeyProvider = {
                        fetchEphemeralKey(snapshot)
                    },
                    setupIntentClientSecretProvider = if (
                        snapshot[CustomerSheetPaymentMethodModeDefinition] == PaymentMethodMode.SetupIntent
                    ) {
                        { customerId -> createSetupIntentClientSecret(snapshot, customerId) }
                    } else {
                        null
                    }
                )
            )
        }
    }

    @OptIn(ExperimentalCustomerSheetApi::class)
    private suspend fun fetchEphemeralKey(
        snapshot: PlaygroundSettings.Snapshot
    ): CustomerAdapter.Result<CustomerEphemeralKey> {
        val requestBody = snapshot.customerEphemeralKeyRequest()

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

                if (snapshot[CustomerSettingsDefinition] == CustomerType.NEW) {
                    playgroundSettingsFlow.value?.let { settings ->
                        updateSettingsWithExistingCustomerId(settings, response.customerId)
                    }
                }

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
    private suspend fun createSetupIntentClientSecret(
        snapshot: PlaygroundSettings.Snapshot,
        customerId: String
    ): CustomerAdapter.Result<String> {
        val request = CreateSetupIntentRequest(
            customerId = customerId,
            merchantCountryCode = snapshot[CountrySettingsDefinition].value,
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

    fun onFlowControllerConfigured(success: Boolean, error: Throwable?) {
        if (success) {
            flowControllerState.value = FlowControllerState()
        } else {
            status.value = StatusMessage(error?.message ?: "Failed to configure flow controller.")
        }
    }

    fun onPaymentOptionSelected(paymentOption: PaymentOption?) {
        flowControllerState.update { existingState ->
            existingState?.copy(
                selectedPaymentOption = paymentOption
            )
        }
    }

    fun onPaymentSheetResult(paymentResult: PaymentSheetResult) {
        if (paymentResult !is PaymentSheetResult.Canceled) {
            state.value = null
        }

        val statusMessage = when (paymentResult) {
            is PaymentSheetResult.Canceled -> {
                "Canceled"
            }

            is PaymentSheetResult.Completed -> {
                SUCCESS_RESULT
            }

            is PaymentSheetResult.Failed -> {
                when (paymentResult.error) {
                    is ConfirmIntentEndpointException -> {
                        "Couldn't process your payment."
                    }

                    is ConfirmIntentNetworkException -> {
                        "No internet. Try again later."
                    }

                    else -> {
                        paymentResult.error.message
                    }
                }
            }
        }

        status.value = StatusMessage(statusMessage)
    }

    private fun createIntent(clientSecret: String): CreateIntentResult {
        // Note: This is not how you'd do this in a real application. Instead, your app would
        // call your backend and create (and optionally confirm) a payment or setup intent.
        return CreateIntentResult.Success(clientSecret = clientSecret)
    }

    suspend fun createIntentCallback(
        paymentMethod: PaymentMethod,
        shouldSavePaymentMethod: Boolean,
    ): CreateIntentResult {
        val playgroundState = state.value?.asPaymentState() ?: return CreateIntentResult.Failure(
            cause = IllegalStateException("No payment playground state"),
            displayMessage = "No payment playground state"
        )
        return createAndConfirmIntent(
            paymentMethodId = paymentMethod.id!!,
            shouldSavePaymentMethod = shouldSavePaymentMethod,
            playgroundState = playgroundState,
        )
    }

    @OptIn(DelicatePaymentSheetApi::class)
    private suspend fun createAndConfirmIntent(
        paymentMethodId: String,
        shouldSavePaymentMethod: Boolean,
        playgroundState: PlaygroundState.Payment,
    ): CreateIntentResult {
        return when (playgroundState.initializationType) {
            InitializationType.Normal -> {
                error("createAndConfirmIntent should not be called when initialization type is Normal")
            }

            InitializationType.DeferredClientSideConfirmation -> {
                createIntent(playgroundState.clientSecret)
            }

            InitializationType.DeferredServerSideConfirmation,
            InitializationType.DeferredManualConfirmation -> {
                createAndConfirmIntentInternal(
                    paymentMethodId = paymentMethodId,
                    shouldSavePaymentMethod = shouldSavePaymentMethod,
                    playgroundState = playgroundState,
                )
            }

            InitializationType.DeferredMultiprocessor -> {
                CreateIntentResult.Success(PaymentSheet.IntentConfiguration.COMPLETE_WITHOUT_CONFIRMING_INTENT)
            }
        }
    }

    private suspend fun createAndConfirmIntentInternal(
        paymentMethodId: String,
        shouldSavePaymentMethod: Boolean,
        playgroundState: PlaygroundState.Payment,
    ): CreateIntentResult {
        // Note: This is not how you'd do this in a real application. You wouldn't have a client
        // secret available at this point, but you'd call your backend to create (and optionally
        // confirm) a payment or setup intent.
        val request = ConfirmIntentRequest(
            clientSecret = playgroundState.clientSecret,
            paymentMethodId = paymentMethodId,
            shouldSavePaymentMethod = shouldSavePaymentMethod,
            merchantCountryCode = playgroundState.countryCode.value,
            mode = playgroundState.checkoutMode.value,
            returnUrl = RETURN_URL,
        )

        val result = Fuel.post(settings.playgroundBackendUrl + "confirm_intent")
            .jsonBody(Json.encodeToString(ConfirmIntentRequest.serializer(), request))
            .suspendable()
            .awaitModel(ConfirmIntentResponse.serializer())
        val createIntentResult = when (result) {
            is Result.Failure -> {
                val message = "Creating intent failed:\n${result.getException().message}"
                status.value = StatusMessage(message)

                val error = if (result.error.cause is IOException) {
                    ConfirmIntentNetworkException()
                } else {
                    ConfirmIntentEndpointException()
                }

                CreateIntentResult.Failure(
                    cause = error,
                    displayMessage = message
                )
            }

            is Result.Success -> {
                CreateIntentResult.Success(
                    clientSecret = result.value.clientSecret,
                )
            }
        }
        return createIntentResult
    }

    fun onAddressLauncherResult(addressLauncherResult: AddressLauncherResult) {
        when (addressLauncherResult) {
            AddressLauncherResult.Canceled -> {
                status.value = StatusMessage("Canceled")
            }

            is AddressLauncherResult.Succeeded -> {
                val addressDetails = addressLauncherResult.address
                playgroundSettingsFlow.value?.set(ShippingAddressSettingsDefinition, addressDetails)
                flowControllerState.update { it?.copy(addressDetails = addressDetails) }
            }
        }
    }

    @OptIn(ExperimentalCustomerSheetApi::class)
    private fun updateSettingsWithExistingCustomerId(
        settings: PlaygroundSettings,
        customerId: String,
    ) {
        settings[CustomerSettingsDefinition] = CustomerType.Existing(customerId)

        playgroundSettingsFlow.value = settings

        state.value = state.value?.let { state ->
            val updatedSnapshot = settings.snapshot()

            when (state) {
                is PlaygroundState.Customer -> state.copy(snapshot = updatedSnapshot)
                is PlaygroundState.Payment -> state.copy(snapshot = updatedSnapshot)
            }
        }
    }

    internal class Factory(
        private val applicationSupplier: () -> Application,
        private val uriSupplier: () -> Uri?,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return PaymentSheetPlaygroundViewModel(applicationSupplier(), uriSupplier()) as T
        }
    }
}

class ConfirmIntentEndpointException : Exception()

class ConfirmIntentNetworkException : Exception()

private const val RETURN_URL = "stripesdk://payment_return_url/" +
    "com.stripe.android.paymentsheet.example"

const val SUCCESS_RESULT = "Success"

data class StatusMessage(
    val message: String?,
    val hasBeenDisplayed: Boolean = false
)
