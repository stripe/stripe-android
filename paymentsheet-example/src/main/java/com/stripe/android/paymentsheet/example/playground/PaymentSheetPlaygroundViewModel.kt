package com.stripe.android.paymentsheet.example.playground

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.result.Result
import com.stripe.android.PaymentConfiguration
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.CustomerEphemeralKey
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetResult
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentelement.AnalyticEvent
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.CreateIntentResult
import com.stripe.android.paymentsheet.DelicatePaymentSheetApi
import com.stripe.android.paymentsheet.ExperimentalCustomerSessionApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.addresselement.AddressLauncherResult
import com.stripe.android.paymentsheet.example.Settings
import com.stripe.android.paymentsheet.example.playground.model.ConfirmIntentRequest
import com.stripe.android.paymentsheet.example.playground.model.ConfirmIntentResponse
import com.stripe.android.paymentsheet.example.playground.model.CreateSetupIntentRequest
import com.stripe.android.paymentsheet.example.playground.model.CreateSetupIntentResponse
import com.stripe.android.paymentsheet.example.playground.model.CustomerEphemeralKeyRequest
import com.stripe.android.paymentsheet.example.playground.model.CustomerEphemeralKeyResponse
import com.stripe.android.paymentsheet.example.playground.network.PlaygroundRequester
import com.stripe.android.paymentsheet.example.playground.settings.Country
import com.stripe.android.paymentsheet.example.playground.settings.CustomEndpointDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.CustomerType
import com.stripe.android.paymentsheet.example.playground.settings.EmbeddedAppearance
import com.stripe.android.paymentsheet.example.playground.settings.EmbeddedAppearanceSettingsDefinition
import com.stripe.android.paymentsheet.example.playground.settings.InitializationType
import com.stripe.android.paymentsheet.example.playground.settings.PlaygroundConfigurationData
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

@OptIn(ExperimentalCustomerSessionApi::class, ExperimentalEmbeddedPaymentElementApi::class)
internal class PaymentSheetPlaygroundViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    launchUri: Uri?,
) : AndroidViewModel(application) {

    private val settings by lazy {
        Settings(application)
    }

    val playgroundSettingsFlow = MutableStateFlow<PlaygroundSettings?>(null)
    val status = MutableStateFlow<StatusMessage?>(null)
    val state = savedStateHandle.getStateFlow<PlaygroundState?>(PLAYGROUND_STATE_KEY, null)
    val flowControllerState = MutableStateFlow<FlowControllerState?>(null)
    val customerSheetState = MutableStateFlow<CustomerSheetState?>(null)

    private val baseUrl: String
        get() {
            val customEndpoint = playgroundSettingsFlow.value?.get(CustomEndpointDefinition)?.value
            return customEndpoint ?: settings.playgroundBackendUrl
        }

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
        setPlaygroundState(null)
        flowControllerState.value = null
        customerSheetState.value = null

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

            PlaygroundRequester(playgroundSettingsSnapshot, getApplication()).fetch().fold(
                onSuccess = { state ->
                    playgroundSettingsFlow.value = state.snapshot.playgroundSettings()
                    setPlaygroundState(state)
                },
                onFailure = { exception ->
                    status.value = StatusMessage(
                        "Preparing checkout failed:\n${exception.message}"
                    )
                },
            )
        }
    }

    private fun prepareCustomer(
        playgroundSettings: PlaygroundSettings
    ) {
        viewModelScope.launch {
            val snapshot = playgroundSettings.snapshot()

            snapshot.saveToSharedPreferences(getApplication())

            customerSheetState.value = CustomerSheetState()

            setPlaygroundState(
                PlaygroundState.Customer(
                    snapshot = snapshot,
                    endpoint = settings.playgroundBackendUrl,
                )
            )
        }
    }

    fun createCustomerAdapter(
        playgroundState: PlaygroundState?,
    ): CustomerAdapter {
        val customerState = playgroundState?.asCustomerState()

        return CustomerAdapter.create(
            context = getApplication(),
            customerEphemeralKeyProvider = {
                customerState?.let { state ->
                    fetchEphemeralKey(state.customerEphemeralKeyRequest(), state.isNewCustomer)
                } ?: throw IllegalStateException("Cannot fetch an ephemeral key!")
            },
            setupIntentClientSecretProvider = if (customerState?.inSetupMode == true) {
                { customerId -> createSetupIntentClientSecret(customerId, customerState.countryCode) }
            } else {
                null
            },
            paymentMethodTypes = customerState?.supportedPaymentMethodTypes,
        )
    }

    fun createCustomerSessionProvider(
        playgroundState: PlaygroundState.Customer,
    ): CustomerSheet.CustomerSessionProvider {
        return object : CustomerSheet.CustomerSessionProvider() {
            override suspend fun intentConfiguration(): kotlin.Result<CustomerSheet.IntentConfiguration> {
                return kotlin.Result.success(
                    CustomerSheet.IntentConfiguration.Builder()
                        .paymentMethodTypes(playgroundState.supportedPaymentMethodTypes)
                        .build()
                )
            }

            override suspend fun providesCustomerSessionClientSecret(): kotlin.Result<
                CustomerSheet.CustomerSessionClientSecret
                > {
                val apiResponse = Fuel.post(baseUrl + "customer_ephemeral_key")
                    .jsonBody(
                        Json.encodeToString(
                            CustomerEphemeralKeyRequest.serializer(),
                            playgroundState.customerEphemeralKeyRequest()
                        )
                    )
                    .suspendable()
                    .awaitModel(CustomerEphemeralKeyResponse.serializer())

                return when (apiResponse) {
                    is Result.Failure -> {
                        val exception = apiResponse.getException()

                        kotlin.Result.failure(exception)
                    }
                    is Result.Success -> {
                        val response = apiResponse.value

                        // Init PaymentConfiguration with the publishable key returned from the backend,
                        // which will be used on all Stripe API calls
                        PaymentConfiguration.init(
                            getApplication(),
                            response.publishableKey
                        )

                        if (playgroundState.isNewCustomer) {
                            playgroundSettingsFlow.value?.let { settings ->
                                updateSettingsWithExistingCustomerId(settings, response.customerId)
                            }
                        }

                        try {
                            kotlin.Result.success(
                                CustomerSheet.CustomerSessionClientSecret.create(
                                    customerId = response.customerId,
                                    clientSecret = response.customerSessionClientSecret
                                        ?: throw IllegalStateException(
                                            "No 'customerSessionClientSecret' was found in backend response!"
                                        )
                                )
                            )
                        } catch (exception: IllegalStateException) {
                            kotlin.Result.failure(exception)
                        }
                    }
                }
            }

            override suspend fun provideSetupIntentClientSecret(customerId: String): kotlin.Result<String> {
                val request = CreateSetupIntentRequest(
                    customerId = customerId,
                    merchantCountryCode = playgroundState.countryCode.value,
                )

                val apiResponse = Fuel.post(baseUrl + "create_setup_intent")
                    .jsonBody(Json.encodeToString(CreateSetupIntentRequest.serializer(), request))
                    .suspendable()
                    .awaitModel(CreateSetupIntentResponse.serializer())

                return when (apiResponse) {
                    is Result.Failure -> kotlin.Result.failure(apiResponse.getException())
                    is Result.Success -> kotlin.Result.success(apiResponse.value.clientSecret)
                }
            }
        }
    }

    private suspend fun fetchEphemeralKey(
        request: CustomerEphemeralKeyRequest,
        isNewCustomer: Boolean,
    ): CustomerAdapter.Result<CustomerEphemeralKey> {
        val apiResponse = Fuel.post(baseUrl + "customer_ephemeral_key")
            .jsonBody(Json.encodeToString(CustomerEphemeralKeyRequest.serializer(), request))
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

                if (isNewCustomer) {
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

    private suspend fun createSetupIntentClientSecret(
        customerId: String,
        country: Country,
    ): CustomerAdapter.Result<String> {
        val request = CreateSetupIntentRequest(
            customerId = customerId,
            merchantCountryCode = country.value,
        )

        val apiResponse = Fuel.post(baseUrl + "create_setup_intent")
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
        val integrationType = playgroundSettingsFlow.value?.configurationData?.value?.integrationType
            ?: PlaygroundConfigurationData.IntegrationType.PaymentSheet
        if (integrationType == PlaygroundConfigurationData.IntegrationType.FlowController) {
            if (paymentResult is PaymentSheetResult.Completed) {
                setPlaygroundState(null)
            }
        } else if (paymentResult !is PaymentSheetResult.Canceled) {
            setPlaygroundState(null)
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
                        "Couldn't process your payment: ${paymentResult.error.message}"
                    }

                    is ConfirmIntentNetworkException -> {
                        "No internet. Try again later."
                    }

                    else -> {
                        "Something went wrong: ${paymentResult.error.message}"
                    }
                }
            }
        }

        status.value = StatusMessage(statusMessage)
    }

    fun onEmbeddedResult(success: Boolean) {
        if (success) {
            setPlaygroundState(null)
            status.value = StatusMessage(SUCCESS_RESULT)
        }
    }

    fun onEmbeddedResult(result: EmbeddedPaymentElement.Result) {
        if (result is EmbeddedPaymentElement.Result.Completed) {
            setPlaygroundState(null)
        }

        val statusMessage = when (result) {
            is EmbeddedPaymentElement.Result.Canceled -> {
                "Canceled"
            }

            is EmbeddedPaymentElement.Result.Completed -> {
                SUCCESS_RESULT
            }

            is EmbeddedPaymentElement.Result.Failed -> {
                when (result.error) {
                    is ConfirmIntentEndpointException -> {
                        "Couldn't process your payment: ${result.error.message}"
                    }

                    is ConfirmIntentNetworkException -> {
                        "No internet. Try again later."
                    }

                    else -> {
                        "Something went wrong: ${result.error.message}"
                    }
                }
            }
        }

        status.value = StatusMessage(statusMessage)
    }

    fun onCustomerSheetCallback(result: CustomerSheetResult) {
        val statusMessage = when (result) {
            is CustomerSheetResult.Canceled -> {
                updatePaymentOptionForCustomerSheet(result.selection?.paymentOption)

                "Canceled"
            }
            is CustomerSheetResult.Selected -> {
                updatePaymentOptionForCustomerSheet(result.selection?.paymentOption)

                null
            }
            is CustomerSheetResult.Failed -> "An error occurred: ${result.exception.message}"
        }

        statusMessage?.let { message ->
            viewModelScope.launch {
                status.emit(StatusMessage(message))
            }
        }
    }

    private suspend fun createIntent(playgroundState: PlaygroundState): CreateIntentResult {
        val playgroundSettingsSnapshot = playgroundState.snapshot
        return PlaygroundRequester(playgroundSettingsSnapshot, getApplication()).fetch().fold(
            onSuccess = { state ->
                playgroundSettingsFlow.value = state.snapshot.playgroundSettings()
                setPlaygroundState(state)
                val clientSecret = requireNotNull(state.asPaymentState()).clientSecret
                CreateIntentResult.Success(clientSecret)
            },
            onFailure = { exception ->
                CreateIntentResult.Failure(IllegalStateException(exception))
            },
        )
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

    @OptIn(ExperimentalAnalyticEventCallbackApi::class)
    fun analyticCallback(event: AnalyticEvent) {
        Log.d("AnalyticEvent", "Event: $event")
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
                createIntent(playgroundState)
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

        val result = Fuel.post(baseUrl + "confirm_intent")
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

    private fun updateSettingsWithExistingCustomerId(
        settings: PlaygroundSettings,
        customerId: String,
    ) {
        settings[CustomerSettingsDefinition] = CustomerType.Existing(customerId)

        playgroundSettingsFlow.value = settings

        setPlaygroundState(
            state.value?.let { state ->
                val updatedSnapshot = settings.snapshot()

                when (state) {
                    is PlaygroundState.Customer -> state.copy(snapshot = updatedSnapshot)
                    is PlaygroundState.Payment -> state.copy(snapshot = updatedSnapshot)
                }
            }
        )
    }

    fun onCustomUrlUpdated(backendUrl: String?) {
        playgroundSettingsFlow.value?.let { settings ->
            settings[CustomEndpointDefinition] = backendUrl
            playgroundSettingsFlow.value = settings
            setPlaygroundState(
                state.value?.let { state ->
                    val updatedSnapshot = settings.snapshot()
                    when (state) {
                        is PlaygroundState.Customer -> state.copy(snapshot = updatedSnapshot)
                        is PlaygroundState.Payment -> state.copy(snapshot = updatedSnapshot)
                    }
                }
            )
        }
    }

    fun updateEmbeddedAppearance(appearanceSetting: EmbeddedAppearanceSettingsDefinition, value: EmbeddedAppearance) {
        playgroundSettingsFlow.value?.let { settings ->
            settings[appearanceSetting] = value
            setPlaygroundState(
                state.value?.let { state ->
                    val updatedSnapshot = settings.snapshot()
                    when (state) {
                        is PlaygroundState.Customer -> state.copy(snapshot = updatedSnapshot)
                        is PlaygroundState.Payment -> state.copy(snapshot = updatedSnapshot)
                    }
                }
            )
        }
    }

    private fun updatePaymentOptionForCustomerSheet(paymentOption: PaymentOption?) {
        customerSheetState.update { existingState ->
            existingState?.copy(
                selectedPaymentOption = paymentOption,
                shouldFetchPaymentOption = false
            )
        }
    }

    private fun setPlaygroundState(state: PlaygroundState?) {
        savedStateHandle[PLAYGROUND_STATE_KEY] = state
    }

    internal class Factory(
        private val applicationSupplier: () -> Application,
        private val uriSupplier: () -> Uri?,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            @Suppress("UNCHECKED_CAST")
            return PaymentSheetPlaygroundViewModel(
                applicationSupplier(),
                extras.createSavedStateHandle(),
                uriSupplier()
            ) as T
        }
    }

    private companion object {
        const val PLAYGROUND_STATE_KEY = "PaymentSheetPlaygroundState"
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
