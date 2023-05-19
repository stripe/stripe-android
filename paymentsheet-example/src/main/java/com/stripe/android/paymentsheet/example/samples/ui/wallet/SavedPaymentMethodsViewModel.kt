package com.stripe.android.paymentsheet.example.samples.ui.wallet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.requests.suspendable
import com.github.kittinunf.result.Result
import com.stripe.android.ExperimentalSavedPaymentMethodsApi
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.example.samples.networking.ExampleSavedPaymentMethodRequest
import com.stripe.android.paymentsheet.example.samples.networking.ExampleSavedPaymentMethodResponse
import com.stripe.android.paymentsheet.example.samples.networking.awaitModel
import com.stripe.android.paymentsheet.repositories.CustomerEphemeralKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSavedPaymentMethodsApi::class)
class SavedPaymentMethodsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _state = MutableStateFlow<SavedPaymentMethodsViewState>(
        value = SavedPaymentMethodsViewState.Loading
    )
    val state: StateFlow<SavedPaymentMethodsViewState> = _state

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            val request = ExampleSavedPaymentMethodRequest(
                customerType = "returning"
            )
            val requestBody = Json.encodeToString(
                ExampleSavedPaymentMethodRequest.serializer(),
                request
            )
            val apiResult = Fuel
                .post("$backendUrl/customer_ephemeral_key")
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
                        SavedPaymentMethodsViewState.Data(
                            customerEphemeralKey = CustomerEphemeralKey.create(
                                customerId = apiResult.value.customerId,
                                ephemeralKey = apiResult.value.customerEphemeralKeySecret,
                            )
                        )
                    }
                }
                is Result.Failure -> {
                    _state.update {
                        SavedPaymentMethodsViewState.FailedToLoad(
                            message = apiResult.error.message.toString()
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
