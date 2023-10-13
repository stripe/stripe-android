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
import com.stripe.android.paymentsheet.example.playground.settings.InitializationType
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

    /**
     * Calls the backend to prepare for checkout. The server creates a new Payment or Setup Intent
     * that will be confirmed on the client using Payment Sheet.
     */
    fun prepareCheckout(
        playgroundSettings: PlaygroundSettings,
    ) {
        state.value = null
        flowControllerState.value = null

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

                    // Init PaymentConfiguration with the publishable key returned from the backend,
                    // which will be used on all Stripe API calls
                    PaymentConfiguration.init(
                        getApplication(),
                        checkoutResponse.publishableKey
                    )

                    state.value = checkoutResponse.asPlaygroundState(
                        snapshot = playgroundSettingsSnapshot,
                    )
                }
            }
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
                "Success"
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
        val playgroundState = state.value ?: return CreateIntentResult.Failure(
            cause = IllegalStateException("No playground state"),
            displayMessage = "No playground state"
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
        playgroundState: PlaygroundState,
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
        playgroundState: PlaygroundState,
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

data class StatusMessage(
    val message: String?,
    val hasBeenDisplayed: Boolean = false
)
